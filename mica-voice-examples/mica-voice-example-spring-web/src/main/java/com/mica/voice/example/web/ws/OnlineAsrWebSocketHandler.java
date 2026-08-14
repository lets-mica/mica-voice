package com.mica.voice.example.web.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.k2fsa.sherpa.onnx.OnlineStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.exception.EngineException;
import net.dreamlu.mica.voice.exception.MicaVoiceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流式 ASR WebSocket Handler。
 *
 * <p>协议（json 控制帧 + binary 音频帧混合）：
 * <pre>
 *   客户端 → 服务端
 *     文本 { "type":"config", "sampleRate":16000, "format":"pcm16le" }
 *     文本 { "type":"start" }
 *     二进制：raw PCM 16-bit signed LE 单声道 chunk（任意大小）
 *     文本 { "type":"stop" }
 *     文本 { "type":"close" }
 *   服务端 → 客户端
 *     { "type":"ready" }
 *     { "type":"partial", "text":"...", "ts":1234 }
 *     { "type":"final",   "text":"...", "ts":1234, "costMs":100 }
 *     { "type":"error", "message":"..." }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineAsrWebSocketHandler extends TextWebSocketHandler {

	private static final int DEFAULT_SAMPLE_RATE = 16000;
	private static final int[] ALLOWED_SAMPLE_RATES = {8000, 16000, 48000};
	/**
	 * 每收到多少个 binary 帧，主动回一个 partial 心跳（即使文本为空），
	 * 让前端能感知到"识别在跑、没有卡住"。
	 */
	private static final int HEARTBEAT_EVERY_N_FRAMES = 8;

	private final ObjectProvider<OnlineAsrService> onlineAsrProvider;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * PCM16-LE → float[] [-1, 1]
	 */
	static float[] pcm16ToFloat(byte[] pcm) {
		if (pcm.length % 2 != 0) {
			throw new MicaVoiceException("pcm16 数据长度必须是 2 的倍数");
		}
		ByteBuffer buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
		int n = pcm.length / 2;
		float[] out = new float[n];
		for (int i = 0; i < n; i++) {
			out[i] = buf.getShort() / 32768.0f;
		}
		return out;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("WS 连接建立: session={}", session.getId());
		// 占位 Holder
		session.getAttributes().put("ctx", new SessionContext());
		send(session, "ready", null);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Object> payload;
		try {
			payload = objectMapper.readValue(message.getPayload(), Map.class);
		} catch (Exception e) {
			sendError(session, "非法 JSON: " + e.getMessage());
			return;
		}
		Object typeObj = payload.get("type");
		if (typeObj == null) {
			sendError(session, "缺少 type 字段");
			return;
		}
		String type = String.valueOf(typeObj);
		switch (type) {
			case "config":
				handleConfig(session, payload);
				break;
			case "start":
				handleStart(session);
				break;
			case "stop":
				handleStop(session);
				break;
			case "close":
				session.close();
				break;
			default:
				sendError(session, "未知 type: " + type);
		}
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
		SessionContext ctx = (SessionContext) session.getAttributes().get("ctx");
		if (ctx == null) {
			sendError(session, "未初始化会话");
			return;
		}
		if (!ctx.started.get()) {
			// 收到 audio 前必须先 start（首次发送才创建 native stream）
			// 这里容忍：客户端可能没显式 start；用 first-frame 隐式 start
			ctx.started.set(true);
			ctx.stream = onlineAsrProvider.getObject().createStream();
		}
		if (ctx.stream == null) {
			sendError(session, "OnlineAsrService 未启用（mica.voice.asr.online.enabled=false）");
			return;
		}
		byte[] payload = message.getPayload().array();
		if (payload.length < 2) {
			// 空帧直接跳过，避免送 0 样本触发 sherpa-onnx 底层断言
			return;
		}
		// PCM16LE → float[]
		float[] samples = pcm16ToFloat(payload);
		ctx.frameCount++;
		try {
			// 关键：必须 while(isReady) 才能 decode，否则 sherpa-onnx 在 Transducer/X-ASR
			// 模型上会因为底层特征帧不足触发 GetFrames CHECK 断言导致 JNI 进程 abort。
			String text = onlineAsrProvider.getObject().feedAndDecode(ctx.stream, samples, ctx.sampleRate).getText();
			String trimmed = text == null ? "" : text.trim();
			// 两条任一满足就发 partial：
			//   a) 文本相对上次有变化（增量或回退）
			//   b) 每收到 HEARTBEAT_EVERY_N_FRAMES 帧强制发一次（即使为空），用于告诉前端"在跑"
			boolean changed = !trimmed.equals(ctx.lastPartial);
			boolean heartbeat = ctx.frameCount % HEARTBEAT_EVERY_N_FRAMES == 0;
			if (changed || heartbeat) {
				Map<String, Object> resp = new LinkedHashMap<>();
				resp.put("type", "partial");
				resp.put("text", trimmed);
				resp.put("ts", System.currentTimeMillis());
				resp.put("frame", ctx.frameCount);
				sendJson(session, resp);
				ctx.lastPartial = trimmed;
			}
		} catch (Throwable t) {
			sendError(session, "解码失败: " + t.getMessage());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		SessionContext ctx = (SessionContext) session.getAttributes().get("ctx");
		if (ctx != null && ctx.stream != null) {
			try {
				ctx.stream.release();
			} catch (Throwable ignore) {
			}
		}
		log.info("WS 连接关闭: session={}, status={}", session.getId(), status);
	}

	// ========================== 子命令处理 ==========================

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.warn("WS 传输错误: session={}, msg={}", session.getId(), exception.getMessage());
	}

	private void handleConfig(WebSocketSession session, Map<String, Object> payload) {
		SessionContext ctx = (SessionContext) session.getAttributes().get("ctx");
		Object sr = payload.get("sampleRate");
		int requested = DEFAULT_SAMPLE_RATE;
		if (sr instanceof Number) {
			requested = ((Number) sr).intValue();
		}
		boolean allowed = false;
		for (int v : ALLOWED_SAMPLE_RATES) {
			if (v == requested) { allowed = true; break; }
		}
		if (!allowed) {
			sendError(session, "不支持的 sampleRate: " + requested + "（仅支持 8000/16000/48000）");
			return;
		}
		ctx.sampleRate = requested;
		Map<String, Object> ack = new LinkedHashMap<>();
		ack.put("type", "config-ack");
		ack.put("sampleRate", ctx.sampleRate);
		sendJson(session, ack);
		log.info("WS 会话配置: session={}, sampleRate={}", session.getId(), ctx.sampleRate);
	}

	private void handleStart(WebSocketSession session) {
		SessionContext ctx = (SessionContext) session.getAttributes().get("ctx");
		if (onlineAsrProvider.getIfAvailable() == null) {
			sendError(session, "OnlineAsrService 未启用");
			return;
		}
		if (ctx.stream != null) {
			ctx.stream.release();
		}
		ctx.stream = onlineAsrProvider.getObject().createStream();
		ctx.started.set(true);
		send(session, "started", null);
	}

	// ========================== helpers ==========================

	private void handleStop(WebSocketSession session) {
		SessionContext ctx = (SessionContext) session.getAttributes().get("ctx");
		if (ctx.stream == null) {
			return;
		}
		try {
			ctx.stream.inputFinished();
			// 持续 decode 直到不再 ready
			while (onlineAsrProvider.getObject().getRecognizer().isReady(ctx.stream)) {
				onlineAsrProvider.getObject().getRecognizer().decode(ctx.stream);
			}
			String text = readStreamText(ctx);
			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("type", "final");
			resp.put("text", text);
			resp.put("ts", System.currentTimeMillis());
			resp.put("costMs", System.currentTimeMillis() - ctx.startMs);
			sendJson(session, resp);
		} catch (Throwable t) {
			sendError(session, "stop 失败: " + t.getMessage());
		} finally {
			ctx.started.set(false);
		}
	}

	private String readStreamText(SessionContext ctx) {
		try {
			com.k2fsa.sherpa.onnx.OnlineRecognizerResult result =
				onlineAsrProvider.getObject().getRecognizer().getResult(ctx.stream);
			String text = result.getText();
			return text == null ? "" : text.trim();
		} catch (Throwable t) {
			throw new EngineException("读取识别结果失败: " + t.getMessage(), t);
		}
	}

	private void send(WebSocketSession session, String type, String text) {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("type", type);
		if (text != null) {
			resp.put("text", text);
		}
		sendJson(session, resp);
	}

	private void sendJson(WebSocketSession session, Object payload) {
		try {
			synchronized (session) {
				session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
			}
		} catch (Exception e) {
			log.warn("WS 发送失败: {}", e.getMessage());
		}
	}

	private void sendError(WebSocketSession session, String message) {
		Map<String, Object> err = new LinkedHashMap<>();
		err.put("type", "error");
		err.put("message", message);
		sendJson(session, err);
	}

	/**
	 * 每个 WS 会话的服务端上下文。
	 */
	private static final class SessionContext {
		final AtomicBoolean started = new AtomicBoolean(false);
		OnlineStream stream;
		int sampleRate = DEFAULT_SAMPLE_RATE;
		long startMs;
		/** 已收到的 binary 帧计数，用于发心跳 */
		int frameCount;
		/** 上一次已发出的 partial 文本（用于去重/变化检测） */
		String lastPartial = "";

		SessionContext() {
			this.startMs = System.currentTimeMillis();
		}
	}
}
