package net.dreamlu.mica.voice.tts;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * sherpa-onnx {@code OfflineTts} 的 mica-voice 适配（VITS）。
 *
 * <p>自动识别模型家族结构：
 * <ul>
 *     <li>HF 系（如 vits-zh-hf-fanchen-C）：model/lexicon/tokens 三个文件</li>
 *     <li>icefall 系（如 vits-icefall-zh-aishell3）：额外带 dict/ 子目录</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
public class OfflineTtsService implements TtsService {

	private static final String[] MODEL_CANDIDATES = {
		"vits-zh-hf-fanchen-C.onnx", "model.onnx", "model.int8.onnx"
	};

	private final MicaVoiceConfig props;
	private final TtsConfig config;
	private final OfflineTts tts;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public OfflineTtsService(MicaVoiceConfig props, TtsConfig config) {
		this.props = props;
		this.config = config;

		if (config.getModelDirName() == null) {
			throw new IllegalArgumentException("TtsConfig.modelDirName 不能为空");
		}
		String modelDir = ModelSelector.resolveModelDir(props.getModelsDir(), config.getModelDirName());
		if (modelDir == null) {
			// 兜底抛错：找不到目录
			ModelSelector.resolveModelFile(props.getModelsDir(), config.getModelDirName(), MODEL_CANDIDATES);
			// 理论上走不到这里
			throw new IllegalStateException("模型目录不存在: " + config.getModelDirName());
		}
		String modelPath = ModelSelector.resolveInDir(modelDir, MODEL_CANDIDATES);
		String lexicon = ModelSelector.resolveInDir(modelDir, "lexicon.txt");
		String tokens = ModelSelector.resolveInDir(modelDir, "tokens.txt");
		if (modelPath == null || lexicon == null || tokens == null) {
			throw new EngineException("TTS 模型目录缺少必要文件 (model/lexicon/tokens): " + modelDir);
		}

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OfflineTtsVitsModelConfig.Builder vitsBuilder = OfflineTtsVitsModelConfig.builder()
			.setModel(modelPath)
			.setLexicon(lexicon)
			.setTokens(tokens);

		// icefall 系模型自带 dict/ 子目录，需额外设置 dictDir / dataDir
		File dictDir = new File(modelDir, "dict");
		if (dictDir.isDirectory()) {
			vitsBuilder.setDictDir(dictDir.getAbsolutePath())
				.setDataDir(modelDir);
			log.info("TTS 检测到 icefall 系 dict 目录: {}", dictDir.getAbsolutePath());
		}

		OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
			.setVits(vitsBuilder.build())
			.setNumThreads(threads)
			.setDebug(debug)
			.build();

		OfflineTtsConfig cfg = OfflineTtsConfig.builder()
			.setModel(modelConfig)
			.build();

		try {
			this.tts = new OfflineTts(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 OfflineTts 失败（model=" + modelPath + "）", t);
		}
		log.info("OfflineTtsService 初始化完成: model={}, sampleRate={}Hz, numSpeakers={}",
			modelPath, tts.getSampleRate(), tts.getNumSpeakers());
	}

	@Override
	public TtsAudio synthesize(String text) {
		return synthesize(text, config.getDefaultSpeakerId(), config.getDefaultSpeed());
	}

	@Override
	public TtsAudio synthesize(String text, int speakerId) {
		return synthesize(text, speakerId, config.getDefaultSpeed());
	}

	@Override
	public TtsAudio synthesize(String text, int speakerId, float speed) {
		ensureOpen();
		long start = System.currentTimeMillis();
		try {
			GeneratedAudio a = tts.generate(text, speakerId, speed);
			long cost = System.currentTimeMillis() - start;
			return new TtsAudio(a.getSamples(), tts.getSampleRate(), speakerId, speed, cost);
		} catch (Throwable t) {
			throw new EngineException("TTS 合成失败: " + t.getMessage(), t);
		}
	}

	@Override
	public TtsAudio synthesizeWithCallback(String text, java.util.function.Consumer<float[]> callback) {
		ensureOpen();
		long start = System.currentTimeMillis();
		int step = config.getCallbackSampleStep() <= 0 ? 1600 : config.getCallbackSampleStep();
		try {
			AtomicInteger acc = new AtomicInteger(0);
			GeneratedAudio a = tts.generateWithCallback(text, config.getDefaultSpeakerId(),
				config.getDefaultSpeed(), samples -> {
					int total = acc.addAndGet(samples.length);
					// 只在累计达到 step 时回调一次（避免微回调刷屏）
					if (total / step > (total - samples.length) / step && callback != null) {
						callback.accept(samples);
					}
				});
			long cost = System.currentTimeMillis() - start;
			return new TtsAudio(a.getSamples(), tts.getSampleRate(),
				config.getDefaultSpeakerId(), config.getDefaultSpeed(), cost);
		} catch (Throwable t) {
			throw new EngineException("TTS 回调式合成失败: " + t.getMessage(), t);
		}
	}

	@Override
	public int getSampleRate() {
		ensureOpen();
		return tts.getSampleRate();
	}

	@Override
	public int getNumSpeakers() {
		ensureOpen();
		return tts.getNumSpeakers();
	}

	/**
	 * 把 TtsAudio 写成 wav 文件。
	 */
	public boolean saveWav(TtsAudio audio, File outFile) {
		ensureOpen();
		try {
			GeneratedAudio generated = new GeneratedAudio(audio.getSamples(), audio.getSampleRate());
			return generated.save(outFile.getAbsolutePath());
		} catch (Throwable t) {
			throw new EngineException("保存 wav 失败: " + outFile.getAbsolutePath(), t);
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OfflineTtsService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				tts.release();
			} catch (Throwable t) {
				log.warn("关闭 OfflineTts 失败: {}", t.getMessage());
			}
		}
	}
}
