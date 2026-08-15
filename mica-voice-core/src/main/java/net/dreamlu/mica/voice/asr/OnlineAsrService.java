package net.dreamlu.mica.voice.asr;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 流式 ASR 服务（基于 sherpa-onnx {@code OnlineRecognizer}）。
 *
 * <p>v1.0：默认支持 Streaming Paraformer（encoder + decoder 拆分）。
 * <br>v1.1：新增 X-ASR（上海交大 Zipformer Transducer，encoder + decoder + joiner 三段式）、
 * + Zipformer / Zipformer2 CTC / NeMo CTC / 通用 Transducer。
 *
 * <p>典型用法：
 * <pre>
 *   try (OnlineAsrService svc = new OnlineAsrService(props, cfg)) {
 *       AudioData audio = AudioReaders.read("input.wav");
 *       AsrResult finalResult = svc.recognizeStreaming(audio, partial -> {
 *           System.out.println("[partial] " + partial.getText());
 *       });
 *   }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
public class OnlineAsrService implements AsrService {

	private final MicaVoiceConfig props;
	private final OnlineAsrConfig config;
	private final OnlineRecognizer recognizer;
	private final int chunkSize;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/**
	 * 构造在线流式 ASR 服务。
	 *
	 * @param props  全局配置（模型根目录、线程数等）
	 * @param config 在线 ASR 配置（必须设置 modelDirName）
	 */
	public OnlineAsrService(MicaVoiceConfig props, OnlineAsrConfig config) {
		this.props = props;
		this.config = config;

		if (config.getModelDirName() == null) {
			throw new IllegalArgumentException("OnlineAsrConfig.modelDirName 不能为空");
		}

		OnlineModelConfig modelConfig = buildOnlineModelConfig();
		FeatureConfig featureConfig = FeatureConfig.builder()
			.setSampleRate(config.getSampleRate())
			.setFeatureDim(config.getFeatureDim())
			.build();
		OnlineRecognizerConfig.Builder cfgBuilder = OnlineRecognizerConfig.builder()
			.setFeatureConfig(featureConfig)
			.setOnlineModelConfig(modelConfig)
			.setEnableEndpoint(config.isEnableEndpoint())
			.setDecodingMethod(config.getDecodingMethod() == null ? "greedy_search" : config.getDecodingMethod());
		// 端点规则（仅在用户显式配置时覆盖，避免影响其他模型族）
		// sherpa-onnx 1.12+ 改用 EndpointRule / EndpointConfig 封装，替代旧的 setRule1/2/3MinXxx。
		EndpointConfig.Builder endpointBuilder = null;
		if (config.getEndpointRule1MinTrailingSilence() != null) {
			endpointBuilder = ensureEndpointBuilder(endpointBuilder)
				.setRule1(EndpointRule.builder()
					.setMinTrailingSilence(config.getEndpointRule1MinTrailingSilence().floatValue())
					.build());
		}
		if (config.getEndpointRule2MinTrailingSilence() != null) {
			endpointBuilder = ensureEndpointBuilder(endpointBuilder)
				.setRule2(EndpointRule.builder()
					.setMinTrailingSilence(config.getEndpointRule2MinTrailingSilence().floatValue())
					.build());
		}
		if (config.getEndpointRule3MinUtteranceLength() != null) {
			endpointBuilder = ensureEndpointBuilder(endpointBuilder)
				.setRule3(EndpointRule.builder()
					.setMinUtteranceLength(config.getEndpointRule3MinUtteranceLength().floatValue())
					.build());
		}
		if (endpointBuilder != null) {
			cfgBuilder.setEndpointConfig(endpointBuilder.build());
		}
		OnlineRecognizerConfig cfg = cfgBuilder.build();

		try {
			this.recognizer = new OnlineRecognizer(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 OnlineRecognizer 失败（modelDir=" + config.getModelDirName()
				+ ", type=" + config.getModelType() + "）", t);
		}
		this.chunkSize = config.getChunkSize() <= 0 ? 1600 : config.getChunkSize();
		log.info("OnlineAsrService 初始化完成: modelDir={}, type={}, chunk={} samples, endpoint={}",
			config.getModelDirName(), config.getModelType(), chunkSize, config.isEnableEndpoint());
	}

	/**
	 * 根据 ModelType 构建 {@link OnlineModelConfig}。
	 */
	private OnlineModelConfig buildOnlineModelConfig() {
		OnlineModelConfig.Builder b = OnlineModelConfig.builder()
			.setTokens(resolveTokens())
			.setNumThreads(config.getThreads() != null ? config.getThreads() : props.getThreads())
			.setDebug(config.isDebug() || props.isDebug());

		String modelTypeString = config.toSherpaModelType();
		if (modelTypeString != null) {
			b.setModelType(modelTypeString);
		}

		switch (config.getModelType() == null ? OnlineAsrConfig.ModelType.AUTO : config.getModelType()) {
			case PARAFORMER: {
				String encoder = resolveFirst(config.getEncoderCandidates());
				String decoder = resolveFirst(config.getDecoderCandidates());
				if (encoder == null || decoder == null) {
					throw new EngineException("PARAFORMER 流式模型缺少 encoder/decoder onnx");
				}
				b.setParaformer(OnlineParaformerModelConfig.builder()
					.setEncoder(encoder)
					.setDecoder(decoder)
					.build());
				break;
			}
			case X_ASR:
			case TRANSDUCER: {
				String encoder = resolveFirst(config.getEncoderCandidates());
				String decoder = resolveFirst(config.getDecoderCandidates());
				String joiner = resolveFirst(config.getJoinerCandidates());
				if (encoder == null || decoder == null || joiner == null) {
					throw new EngineException("Transducer 模型（X_ASR / TRANSDUCER）缺少 encoder/decoder/joiner onnx");
				}
				b.setTransducer(OnlineTransducerModelConfig.builder()
					.setEncoder(encoder)
					.setDecoder(decoder)
					.setJoiner(joiner)
					.build());
				break;
			}
			case ZIPFORMER:
			case ZIPFORMER2_CTC: {
				String model = resolveFirst(config.getEncoderCandidates());
				if (model == null) {
					throw new EngineException("Zipformer CTC 模型缺少 onnx（zipformer2 / zipformer）");
				}
				b.setZipformer2Ctc(com.k2fsa.sherpa.onnx.OnlineZipformer2CtcModelConfig.builder()
					.setModel(model)
					.build());
				break;
			}
			case NEMO_CTC: {
				String model = resolveFirst(config.getEncoderCandidates());
				if (model == null) {
					throw new EngineException("NeMo CTC 模型缺少 onnx");
				}
				b.setNeMoCtc(OnlineNeMoCtcModelConfig.builder()
					.setModel(model)
					.build());
				break;
			}
			case AUTO:
			default: {
				// 尝试按 encoder/decoder/joiner 全部找到 → 走 transducer；否则按 paraformer
				String encoder = resolveFirst(config.getEncoderCandidates());
				String decoder = resolveFirst(config.getDecoderCandidates());
				String joiner = resolveFirst(config.getJoinerCandidates());
				if (encoder != null && decoder != null && joiner != null) {
					b.setTransducer(OnlineTransducerModelConfig.builder()
						.setEncoder(encoder).setDecoder(decoder).setJoiner(joiner).build());
				} else if (encoder != null && decoder != null) {
					b.setParaformer(OnlineParaformerModelConfig.builder()
						.setEncoder(encoder).setDecoder(decoder).build());
				} else {
					throw new EngineException("模型目录下找不到 encoder/decoder/joiner 任何文件: "
						+ config.getModelDirName());
				}
				break;
			}
		}
		return b.build();
	}

	/**
	 * 在模型目录下找 tokens.txt。
	 */
	private String resolveTokens() {
		String tokens = ModelSelector.resolveInDir(
			new File(props.getModelsDir(), config.getModelDirName()).getAbsolutePath(),
			"tokens.txt");
		if (tokens == null) {
			throw new EngineException("模型目录缺少 tokens.txt: " + config.getModelDirName());
		}
		return tokens;
	}

	/**
	 * 在模型目录下依次尝试每个候选名，返回第一个存在的文件路径。
	 */
	private String resolveFirst(String[] candidates) {
		if (candidates == null || candidates.length == 0) {
			return null;
		}
		File modelDir = new File(props.getModelsDir(), config.getModelDirName());
		for (String name : candidates) {
			File f = new File(modelDir, name);
			if (f.isFile()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * 把整段音频按 chunkSize 切块送入，逐块回调 partial，结束后返回最终结果。
	 */
	public AsrResult recognizeStreaming(AudioData audio, Consumer<AsrResult> partial) {
		ensureOpen();
		long start = System.currentTimeMillis();
		OnlineStream stream = recognizer.createStream();
		try {
			float[] samples = audio.getSamples();
			int sampleRate = audio.getSampleRate();
			for (int i = 0; i < samples.length; i += chunkSize) {
				int end = Math.min(i + chunkSize, samples.length);
				float[] chunk = new float[end - i];
				System.arraycopy(samples, i, chunk, 0, chunk.length);
				stream.acceptWaveform(chunk, sampleRate);
				// 对 Transducer（如 X-ASR）必须先 isReady 再 decode，避免 C++ 层帧不足崩溃
				while (recognizer.isReady(stream)) {
					recognizer.decode(stream);
				}
				if (partial != null) {
					OnlineRecognizerResult r = recognizer.getResult(stream);
					String text = r.getText() == null ? "" : r.getText().trim();
					if (!text.isEmpty()) {
						partial.accept(new AsrResult(text, new ArrayList<>(),
							null, null, null, 0));
					}
				}
			}
			stream.inputFinished();
			while (recognizer.isReady(stream)) {
				recognizer.decode(stream);
			}
			OnlineRecognizerResult r = recognizer.getResult(stream);
			long cost = System.currentTimeMillis() - start;
			return new AsrResult(
				r.getText() == null ? "" : r.getText().trim(),
				new ArrayList<>(),
				null, null, null, cost);
		} catch (Throwable t) {
			throw new EngineException("流式识别失败: " + t.getMessage(), t);
		} finally {
			stream.release();
		}
	}

	/**
	 * 创建一个新的流式 stream（适合持续喂入麦克风数据）。
	 */
	public OnlineStream createStream() {
		ensureOpen();
		return recognizer.createStream();
	}

	/**
	 * 把单个 chunk 喂给 stream 并解码。返回最新的文本结果。
	 * 调用方负责 stream 的 release。
	 */
	public OnlineRecognizerResult feedAndDecode(OnlineStream stream, float[] samples, int sampleRate) {
		ensureOpen();
		stream.acceptWaveform(samples, sampleRate);
		while (recognizer.isReady(stream)) {
			recognizer.decode(stream);
		}
		return recognizer.getResult(stream);
	}

	/**
	 * 获取底层 recognizer（高级用法，如自定义 endpoint 逻辑）。
	 */
	public OnlineRecognizer getRecognizer() {
		ensureOpen();
		return recognizer;
	}

	@Override
	public AsrResult recognize(String wavPath) {
		return recognize(AudioReaders.read(wavPath));
	}

	@Override
	public AsrResult recognize(File wav) {
		return recognize(AudioReaders.read(wav));
	}

	@Override
	public AsrResult recognize(AudioData audio) {
		return recognizeStreaming(audio, null);
	}

	/**
	 * 确保服务未被关闭，否则抛出 IllegalStateException。
	 */
	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OnlineAsrService 已关闭");
		}
	}

	/**
	 * 首次使用 endpoint 规则时新建 Builder，否则原样返回。
	 */
	private static EndpointConfig.Builder ensureEndpointBuilder(EndpointConfig.Builder existing) {
		return existing != null ? existing : EndpointConfig.builder();
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				recognizer.release();
			} catch (Throwable t) {
				log.warn("关闭 OnlineRecognizer 失败: {}", t.getMessage());
			}
		}
	}
}
