package com.mica.voice.example.web.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流式 TTS WebSocket Handler。
 *
 * <p><b>背景</b>：sherpa-onnx 的 TTS 引擎是 {@code OfflineTts}（整句合成的离线模型），
 * 没有真正的流式类。本 Handler 用"按句切分 + 逐句合成 + 队列流水线"模拟流式：
 * 客户端（LLM 侧）边输入文本增量，服务端收到一个完整句子就立即合成出声，
 * 不必等整段文本发完。
 *
 * <p><b>架构：每连接一条三线程流水线</b>
 * <pre>
 * WS IO 线程（收文本）          合成线程（自建）               发送线程（自建）
 * JSON 解析 + 切句          tts.synthesize(句)          唯一的 session 写入者
 *      │                          │ 阻塞引擎数百 ms~数秒         │
 *      ▼ 完整句子                  ▼ 每句切 ~100ms PCM16 帧       ▼
 *  textQueue(256) ──────▶ 合成线程 ──────────────▶ frameQueue(64) ──▶ 客户端
 * </pre>
 *
 * <ul>
 *     <li>收文本：运行在容器 IO 线程，只做 JSON 解析 + 切句 + 入队，立即返回，绝不碰引擎。</li>
 *     <li>合成线程：{@link TtsService#synthesize} 是同步阻塞调用、一次独占 CPU 数百毫秒到数秒，
 *         绝不能放进 IO 线程，否则拖垮整个服务。</li>
 *     <li>发送线程：保证音频块与 done 等文本消息不乱序；且是 {@link WebSocketSession} 的
 *         唯一写入者（因此不需要 ConcurrentWebSocketSessionDecorator）。</li>
 *     <li>背压：frameQueue 容量 64，满时合成线程阻塞在入队上 → 不再消费 textQueue →
 *         textQueue(256) 满时 IO 线程阻塞 → 反压到客户端，内存有界不 OOM。</li>
 * </ul>
 *
 * <p>协议（JSON 控制帧 + binary 音频帧）：
 * <pre>
 *   客户端 → 服务端（文本）
 *     { "type":"config", "speakerId":0, "speed":1.0 }  可选，切句前设置合成参数
 *     { "type":"text", "text":"..." }                  增量文本（模拟 LLM 逐 token），可连续多次
 *     { "type":"flush" }                               本轮文本结束，结算缓冲区残余文本
 *     { "type":"close" }                               断开连接
 *   服务端 → 客户端
 *     { "type":"ready", "sampleRate":..., "numSpeakers":..., "textQueueCapacity":256, "frameQueueCapacity":64 }
 *     binary：单声道 16-bit signed little-endian PCM 块（无 WAV 头），~100ms/块
 *     { "type":"done", "costMs":... }                  在最后一段音频之后发出
 *     { "type":"error", "message":"..." }
 * </pre>
 *
 * <p>连接可多轮对话：反复发 {@code text → flush}，服务端每轮 flush 后回一个 {@code done}。
 *
 * @author dreamlu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingTtsWebSocketHandler extends TextWebSocketHandler {

	/**
	 * 句子队列容量：IO 线程（收文本）→ 合成线程。
	 */
	static final int TEXT_QUEUE_CAPACITY = 256;
	/**
	 * 帧队列容量：合成线程 → 发送线程，满时形成背压。
	 */
	static final int FRAME_QUEUE_CAPACITY = 64;
	/**
	 * 每个音频块时长（毫秒），约 100ms 一帧。
	 */
	static final int FRAME_MS = 100;
	/**
	 * 等待入队的最长阻塞时间（毫秒），用于在连接关闭时兜底退出。
	 */
	private static final long ENQUEUE_TIMEOUT_MS = 500;

	/**
	 * 文本队列哨兵：本轮文本结束（flush），合成线程处理完此前所有句子后放一个 done。
	 */
	private static final Object END_OF_TURN = new Object();
	/**
	 * 队列哨兵：关闭流水线。
	 */
	private static final Object SHUTDOWN = new Object();

	private final ObjectProvider<TtsService> ttsProvider;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		TtsService tts = ttsProvider.getIfAvailable();
		if (tts == null) {
			log.warn("WS 连接被拒: session={}, TtsService 未启用", session.getId());
			sendErrorQuietly(session, "TtsService 未启用（mica.voice.tts.enabled=false）");
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("TTS disabled"));
			return;
		}
		StreamingTtsSession ctx = new StreamingTtsSession(session, tts, objectMapper);
		session.getAttributes().put("streamingTtsCtx", ctx);
		ctx.start();
		log.info("WS 流式 TTS 连接建立: session={}, sampleRate={}Hz, numSpeakers={}",
			session.getId(), tts.getSampleRate(), tts.getNumSpeakers());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		StreamingTtsSession ctx = (StreamingTtsSession) session.getAttributes().get("streamingTtsCtx");
		if (ctx == null || ctx.isClosed()) {
			return;
		}
		Map<String, Object> payload;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed = objectMapper.readValue(message.getPayload(), Map.class);
			payload = parsed;
		} catch (Exception e) {
			ctx.enqueueError("非法 JSON: " + e.getMessage());
			return;
		}
		Object typeObj = payload.get("type");
		if (typeObj == null) {
			ctx.enqueueError("缺少 type 字段");
			return;
		}
		String type = String.valueOf(typeObj);
		switch (type) {
			case "config":
				handleConfig(ctx, payload);
				break;
			case "text":
				handleText(ctx, payload);
				break;
			case "flush":
				handleFlush(ctx);
				break;
			case "close":
				closeQuietly(session);
				break;
			default:
				ctx.enqueueError("未知 type: " + type);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		StreamingTtsSession ctx = (StreamingTtsSession) session.getAttributes().get("streamingTtsCtx");
		if (ctx != null) {
			ctx.shutdown();
		}
		log.info("WS 流式 TTS 连接关闭: session={}, status={}", session.getId(), status);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.warn("WS 流式 TTS 传输错误: session={}, msg={}", session.getId(), exception.getMessage());
		StreamingTtsSession ctx = (StreamingTtsSession) session.getAttributes().get("streamingTtsCtx");
		if (ctx != null) {
			ctx.shutdown();
		}
	}

	// ========================== 子命令处理（运行在容器 IO 线程） ==========================

	/**
	 * config：设置说话人 id 与语速，对后续句子生效。
	 */
	private void handleConfig(StreamingTtsSession ctx, Map<String, Object> payload) {
		Object sid = payload.get("speakerId");
		Object speed = payload.get("speed");
		if (sid instanceof Number) {
			int v = ((Number) sid).intValue();
			if (v < 0 || v >= ctx.numSpeakers) {
				ctx.enqueueError("speakerId 越界: " + v + "（0 ~ " + (ctx.numSpeakers - 1) + "）");
				return;
			}
			ctx.speakerId = v;
		}
		if (speed instanceof Number) {
			float v = ((Number) speed).floatValue();
			if (v <= 0f || v > 10f) {
				ctx.enqueueError("speed 非法: " + v + "（应 > 0 且 <= 10）");
				return;
			}
			ctx.speed = v;
		}
		Map<String, Object> ack = new LinkedHashMap<>();
		ack.put("type", "config-ack");
		ack.put("speakerId", ctx.speakerId);
		ack.put("speed", ctx.speed);
		ctx.enqueueFrame(ack);
		log.info("WS 会话配置: session={}, speakerId={}, speed={}", ctx.session.getId(), ctx.speakerId, ctx.speed);
	}

	/**
	 * text：文本增量入缓冲 → 凑出完整句子立即入队（队满则阻塞 = 背压）。
	 */
	private void handleText(StreamingTtsSession ctx, Map<String, Object> payload) {
		Object text = payload.get("text");
		if (text == null || String.valueOf(text).isEmpty()) {
			ctx.enqueueError("text 不能为空");
			return;
		}
		ctx.appendText(String.valueOf(text));
	}

	/**
	 * flush：本轮文本结束，结算残余文本后放 END_OF_TURN，触发 done。
	 */
	private void handleFlush(StreamingTtsSession ctx) {
		ctx.turnStartMs = System.currentTimeMillis();
		ctx.flushTurn();
	}

	// ========================== helpers ==========================

	private void sendErrorQuietly(WebSocketSession session, String message) {
		try {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("type", "error");
			err.put("message", message);
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
		} catch (Exception e) {
			log.warn("WS 发送失败: {}", e.getMessage());
		}
	}

	private void closeQuietly(WebSocketSession session) {
		try {
			session.close();
		} catch (Exception e) {
			log.warn("WS close 失败: {}", e.getMessage());
		}
	}

	/**
	 * 每个 WS 会话的流水线：textQueue + 合成线程 + frameQueue + 发送线程。
	 */
	private static final class StreamingTtsSession {

		private final WebSocketSession session;
		private final TtsService tts;
		private final ObjectMapper objectMapper;
		private final SentenceBuffer buffer = new SentenceBuffer();
		private final ArrayBlockingQueue<Object> textQueue =
			new ArrayBlockingQueue<>(TEXT_QUEUE_CAPACITY);
		private final ArrayBlockingQueue<Object> frameQueue =
			new ArrayBlockingQueue<>(FRAME_QUEUE_CAPACITY);
		private final AtomicBoolean closed = new AtomicBoolean(false);

		private final int sampleRate;
		private final int numSpeakers;
		/**
		 * 当前说话人 id / 语速（IO 线程写、合成线程读，volatile 保证可见性）。
		 */
		private volatile int speakerId = 0;
		private volatile float speed = 1.0f;
		/**
		 * 本轮 flush 到达时间（毫秒），发送 done 时算耗时。
		 */
		private volatile long turnStartMs;

		private Thread synthThread;
		private Thread sendThread;

		StreamingTtsSession(WebSocketSession session, TtsService tts, ObjectMapper objectMapper) {
			this.session = session;
			this.tts = tts;
			this.objectMapper = objectMapper;
			this.sampleRate = tts.getSampleRate();
			this.numSpeakers = tts.getNumSpeakers();
		}

		boolean isClosed() {
			return closed.get();
		}

		/**
		 * 启动流水线：ready 帧先入 frameQueue，再启动合成线程与发送线程。
		 */
		void start() {
			Map<String, Object> ready = new LinkedHashMap<>();
			ready.put("type", "ready");
			ready.put("sampleRate", sampleRate);
			ready.put("numSpeakers", numSpeakers);
			ready.put("textQueueCapacity", TEXT_QUEUE_CAPACITY);
			ready.put("frameQueueCapacity", FRAME_QUEUE_CAPACITY);
			// ready 必须先于一切输出，先入队再启动发送线程即可保证
			frameQueue.offer(ready);

			String prefix = session.getId();
			synthThread = new Thread(this::synthLoop, "tts-synth-" + prefix);
			synthThread.setDaemon(true);
			sendThread = new Thread(this::sendLoop, "tts-send-" + prefix);
			sendThread.setDaemon(true);
			synthThread.start();
			sendThread.start();
		}

		/**
		 * IO 线程调用：追加文本增量并把凑出的完整句子送入 textQueue。
		 * 队满时阻塞（最长 {@link #ENQUEUE_TIMEOUT_MS} 轮询一次）形成背压。
		 */
		void appendText(String text) {
			buffer.append(text);
			for (String sentence : buffer.pollSentences()) {
				enqueue(textQueue, sentence);
			}
		}

		/**
		 * IO 线程调用（flush）：把残余文本作为最后一句入队，再放 END_OF_TURN。
		 */
		void flushTurn() {
			for (String sentence : buffer.flushPending()) {
				enqueue(textQueue, sentence);
			}
			enqueue(textQueue, END_OF_TURN);
		}

		/**
		 * IO 线程调用：把一个错误文本帧送入发送队列（保持 session 单写入者）。
		 */
		void enqueueError(String message) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("type", "error");
			err.put("message", message);
			enqueueFrame(err);
		}

		// ========================== 合成线程 ==========================

		/**
		 * 合成线程主循环：逐句取文本 → 阻塞调用 tts.synthesize → 切帧入 frameQueue。
		 * 遇到 END_OF_TURN 表示本轮句子全部合成完毕，其音频帧已全部入队，
		 * 此时再放一个 done 帧，保证 done 一定排在最后一段音频之后。
		 */
		private void synthLoop() {
			log.info("合成线程启动: session={}", session.getId());
			while (!closed.get()) {
				Object item;
				try {
					item = textQueue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				if (item == SHUTDOWN) {
					break;
				}
				if (item == END_OF_TURN) {
					Map<String, Object> done = new LinkedHashMap<>();
					done.put("type", "done");
					enqueueFrame(done);
					continue;
				}
				String sentence = (String) item;
				try {
					// 同步阻塞：一次独占 CPU 数百毫秒到数秒，只能在本线程调用
					long t0 = System.currentTimeMillis();
					TtsAudio audio = tts.synthesize(sentence, speakerId, speed);
					long cost = System.currentTimeMillis() - t0;
					log.debug("TTS 合成完成: session={}, 句=[{}], {}ms, {} samples",
						session.getId(), sentence, cost, audio.getSamples().length);
					pushAudioFrames(audio);
				} catch (Throwable t) {
					log.warn("TTS 合成失败: session={}, 句=[{}], msg={}",
						session.getId(), sentence, t.getMessage());
					enqueueError("合成失败: " + t.getMessage());
				}
			}
			log.info("合成线程退出: session={}", session.getId());
		}

		/**
		 * 把一句音频切成 ~{@link #FRAME_MS}ms 的 PCM16 小块逐个入 frameQueue。
		 * frameQueue 满时 put 阻塞 → 合成侧停止 → 反压整条流水线。
		 */
		private void pushAudioFrames(TtsAudio audio) {
			float[] samples = audio.getSamples();
			if (samples.length == 0) {
				return;
			}
			int frameSamples = Math.max(1, sampleRate * FRAME_MS / 1000);
			for (int from = 0; from < samples.length; from += frameSamples) {
				int to = Math.min(from + frameSamples, samples.length);
				byte[] pcm = toPcm16(samples, from, to);
				enqueueFrame(pcm);
			}
		}

		/**
		 * float[] [-1,1] 切片 → 16-bit signed little-endian PCM bytes。
		 */
		private static byte[] toPcm16(float[] samples, int from, int to) {
			byte[] out = new byte[(to - from) * 2];
			int p = 0;
			for (int i = from; i < to; i++) {
				float s = samples[i];
				if (s > 1f) {
					s = 1f;
				} else if (s < -1f) {
					s = -1f;
				}
				short v = (short) (s * 32767f);
				out[p++] = (byte) (v & 0xff);
				out[p++] = (byte) ((v >> 8) & 0xff);
			}
			return out;
		}

		// ========================== 发送线程 ==========================

		/**
		 * 发送线程主循环：frameQueue 的唯一消费者、session 的唯一写入者。
		 * byte[] → 音频二进制帧；Map → JSON 文本帧。
		 */
		private void sendLoop() {
			log.info("发送线程启动: session={}", session.getId());
			while (!closed.get()) {
				Object frame;
				try {
					frame = frameQueue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				if (frame == SHUTDOWN) {
					break;
				}
				try {
					if (frame instanceof byte[]) {
						session.sendMessage(new BinaryMessage((byte[]) frame));
					} else if (frame instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> msg = (Map<String, Object>) frame;
						if ("done".equals(msg.get("type"))) {
							msg.put("costMs", System.currentTimeMillis() - turnStartMs);
						}
						session.sendMessage(new TextMessage(
							objectMapper.writeValueAsString(msg)));
					}
				} catch (Exception e) {
					// 发送失败 = 连接已断，退出并触发清理
					log.warn("WS 发送失败，发送线程退出: session={}, msg={}",
						session.getId(), e.getMessage());
					break;
				}
			}
			log.info("发送线程退出: session={}", session.getId());
			shutdown();
		}

		// ========================== 队列 helpers ==========================

		/**
		 * 有界入队（带超时轮询 + closed 检查），IO/合成线程均可用，避免连接关闭时永久阻塞。
		 */
		private void enqueue(ArrayBlockingQueue<Object> queue, Object item) {
			if (item == null) {
				return;
			}
			while (!closed.get()) {
				try {
					if (queue.offer(item, ENQUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
						return;
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		/**
		 * 音频帧/控制帧入 frameQueue（合成线程或 IO 线程调用）。
		 */
		private void enqueueFrame(Object frame) {
			enqueue(frameQueue, frame);
		}

		/**
		 * 关闭流水线：置 closed、清空队列（解开阻塞入队）、发哨兵、中断并 join 工作线程。
		 */
		void shutdown() {
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			textQueue.clear();
			frameQueue.clear();
			textQueue.offer(SHUTDOWN);
			frameQueue.offer(SHUTDOWN);
			interruptAndJoin(synthThread);
			interruptAndJoin(sendThread);
		}

		private void interruptAndJoin(Thread t) {
			if (t == null || t == Thread.currentThread()) {
				return;
			}
			t.interrupt();
			try {
				t.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
