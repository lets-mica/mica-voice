package net.dreamlu.mica.voice.asr;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx {@code OfflineRecognizer} 的 mica-voice 适配。
 *
 * <p>负责：
 * <ul>
 *     <li>根据 {@link AsrConfig} + {@link MicaVoiceConfig} 构造 native 识别器</li>
 *     <li>支持 7 种模型家族（PARAFORMER / SENSE_VOICE / WHISPER / MOONSHINE /
 *         ZIPFORMER / NEMO_CTC / AUTO），根据 {@link AsrConfig#getModelType()} 选择对应配置</li>
 *     <li>int8 模型优先，回退到 fp32</li>
 *     <li>返回统一的 {@link AsrResult}</li>
 *     <li>线程安全：识别器本身复用，仅每次识别创建独立 {@code OfflineStream}</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
public class OfflineAsrService implements AsrService {

	private static final String[] MODEL_CANDIDATES = {
		"model.int8.onnx", "model.onnx"
	};

	private final AsrConfig config;
	private final OfflineRecognizer recognizer;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public OfflineAsrService(MicaVoiceConfig props, AsrConfig config) {
		this.config = config;

		if (config.getModelDirName() == null) {
			throw new IllegalArgumentException("AsrConfig.modelDirName 不能为空");
		}
		String modelPath = ModelSelector.resolveModelFile(
			props.getModelsDir(), config.getModelDirName(), MODEL_CANDIDATES);
		String tokens = ModelSelector.resolveModelFile(
			props.getModelsDir(), config.getModelDirName(), "tokens.txt");

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OfflineModelConfig.Builder modelBuilder = OfflineModelConfig.builder()
			.setTokens(tokens)
			.setNumThreads(threads)
			.setDebug(debug);

		applyModelSpecificConfig(modelBuilder, modelPath);

		OfflineRecognizerConfig cfg = OfflineRecognizerConfig.builder()
			.setOfflineModelConfig(modelBuilder.build())
			.build();

		try {
			this.recognizer = new OfflineRecognizer(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 OfflineRecognizer 失败（模型: " + modelPath + "）", t);
		}
		log.info("OfflineAsrService 初始化完成: model={}, tokens={}, threads={}", modelPath, tokens, threads);
	}

	/**
	 * 简单的 Whisper decoder 路径推导：把 .encoder.onnx → .decoder.onnx
	 */
	private static String deriveWhisperDecoder(String encoderPath) {
		if (encoderPath == null) return null;
		if (encoderPath.endsWith(".encoder.onnx")) {
			return encoderPath.substring(0, encoderPath.length() - ".encoder.onnx".length()) + ".decoder.onnx";
		}
		// 兜底：尝试同名 + ".decoder.onnx"
		int dot = encoderPath.lastIndexOf('.');
		if (dot > 0) {
			return encoderPath.substring(0, dot) + ".decoder.onnx";
		}
		return encoderPath + ".decoder.onnx";
	}

	private void applyModelSpecificConfig(OfflineModelConfig.Builder b, String modelPath) {
		AsrConfig.ModelType type = config.getModelType();
		if (type == null || type == AsrConfig.ModelType.AUTO) {
			// AUTO：仅设置 modelType + tokens，由 sherpa-onnx 自动识别家族
			// 这里不做特别配置，依赖 sherpa-onnx 1.13+ 的自动推断
			return;
		}
		switch (type) {
			case PARAFORMER:
				b.setParaformer(OfflineParaformerModelConfig.builder()
					.setModel(modelPath)
					.build());
				break;
			case SENSE_VOICE:
				b.setSenseVoice(OfflineSenseVoiceModelConfig.builder()
					.setModel(modelPath)
					.setLanguage(config.getLanguage() == null ? "auto" : config.getLanguage())
					.setInverseTextNormalization(config.isInverseTextNormalization())
					.build());
				break;
			case WHISPER:
				// Whisper 是 encoder/decoder 分离结构，传入 modelPath 作为 encoder，相同文件名+.decoder 作为 decoder
				b.setWhisper(OfflineWhisperModelConfig.builder()
					.setEncoder(modelPath)
					.setDecoder(deriveWhisperDecoder(modelPath))
					.setLanguage(config.getLanguage() == null ? "auto" : config.getLanguage())
					.build());
				break;
			case MOONSHINE:
				// Moonshine 也是 encoder/decoder 分离
				b.setMoonshine(OfflineMoonshineModelConfig.builder()
					.setPreprocessor(modelPath + ".preprocessor.onnx")
					.setEncoder(modelPath + ".encoder.onnx")
					.setUncachedDecoder(modelPath + ".uncached_decoder.onnx")
					.setCachedDecoder(modelPath + ".cached_decoder.onnx")
					.build());
				break;
			case ZIPFORMER:
				b.setZipformerCtc(OfflineZipformerCtcModelConfig.builder()
					.setModel(modelPath)
					.build());
				break;
			case NEMO_CTC:
				b.setNemo(OfflineNemoEncDecCtcModelConfig.builder()
					.setModel(modelPath)
					.build());
				break;
			default:
				// 留空
		}
	}

	@Override
	public AsrResult recognize(String wavPath) {
		ensureOpen();
		AudioData audio = AudioReaders.read(wavPath);
		return doRecognize(audio);
	}

	@Override
	public AsrResult recognize(File wav) {
		ensureOpen();
		AudioData audio = AudioReaders.read(wav);
		return doRecognize(audio);
	}

	@Override
	public AsrResult recognize(AudioData audio) {
		ensureOpen();
		return doRecognize(audio);
	}

	private AsrResult doRecognize(AudioData audio) {
		long start = System.currentTimeMillis();
		OfflineStream stream = recognizer.createStream();
		try {
			stream.acceptWaveform(audio.getSamples(), audio.getSampleRate());
			recognizer.decode(stream);
			OfflineRecognizerResult r = recognizer.getResult(stream);
			long cost = System.currentTimeMillis() - start;

			String[] tokens = r.getTokens();
			List<String> tokenList = new ArrayList<>();
			if (tokens != null) {
				java.util.Collections.addAll(tokenList, tokens);
			}
			return new AsrResult(
				r.getText(),
				tokenList,
				r.getLang(),
				r.getEmotion(),
				r.getEvent(),
				cost);
		} catch (Throwable t) {
			throw new EngineException("识别失败: " + t.getMessage(), t);
		} finally {
			stream.release();
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OfflineAsrService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				recognizer.release();
			} catch (Throwable t) {
				log.warn("关闭 OfflineRecognizer 失败: {}", t.getMessage());
			}
		}
	}
}
