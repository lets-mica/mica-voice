package net.dreamlu.mica.voice.kws;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.config.KwsConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx KeywordSpotter 的 mica-voice 适配（KWS / 关键词唤醒）。
 *
 * <p>模型要求：transducer 结构（encoder + decoder + joiner）。
 *
 * @author dreamlu
 */
@Slf4j
public class KeywordSpotterService implements KwsService {

	private final MicaVoiceConfig props;
	private final KwsConfig config;
	private final KeywordSpotter spotter;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public KeywordSpotterService(MicaVoiceConfig props, KwsConfig config) {
		this.props = props;
		this.config = config;

		String modelDir = new File(props.getModelsDir(), config.getModelDirName()).getAbsolutePath();
		String encoder = ModelFileUtil.firstExisting(modelDir,
			"encoder.int8.onnx", "encoder.onnx");
		String decoder = ModelFileUtil.firstExisting(modelDir,
			"decoder.int8.onnx", "decoder.onnx");
		String joiner = ModelFileUtil.firstExisting(modelDir,
			"joiner.int8.onnx", "joiner.onnx");
		String tokens = ModelFileUtil.firstExisting(modelDir, "tokens.txt");
		String keywordsFile = new File(modelDir, config.getKeywordsFile()).getAbsolutePath();

		if (encoder == null || decoder == null || joiner == null || tokens == null) {
			throw new EngineException("KWS 模型目录缺少必要文件 (encoder/decoder/joiner/tokens): " + modelDir);
		}

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OnlineTransducerModelConfig transducer = OnlineTransducerModelConfig.builder()
			.setEncoder(encoder)
			.setDecoder(decoder)
			.setJoiner(joiner)
			.build();
		OnlineModelConfig onlineModel = OnlineModelConfig.builder()
			.setTransducer(transducer)
			.setTokens(tokens)
			.setNumThreads(threads)
			.setDebug(debug)
			.build();
		FeatureConfig featureConfig = FeatureConfig.builder()
			.setSampleRate(config.getSampleRate())
			.setFeatureDim(config.getFeatureDim())
			.build();

		KeywordSpotterConfig cfg = KeywordSpotterConfig.builder()
			.setOnlineModelConfig(onlineModel)
			.setFeatureConfig(featureConfig)
			.setMaxActivePaths(config.getMaxActivePaths())
			.setKeywordsFile(keywordsFile)
			.setKeywordsScore(config.getKeywordsScore())
			.setKeywordsThreshold(config.getKeywordsThreshold())
			.build();

		try {
			this.spotter = new KeywordSpotter(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 KeywordSpotter 失败（modelDir=" + modelDir + "）", t);
		}
		log.info("KeywordSpotter 初始化完成: modelDir={}, keywordsFile={}", modelDir, keywordsFile);
	}

	@Override
	public List<KwsResult> spot(AudioData audio) {
		ensureOpen();
		OnlineStream stream = spotter.createStream();
		try {
			stream.acceptWaveform(audio.getSamples(), audio.getSampleRate());
			stream.inputFinished();
			while (spotter.isReady(stream)) {
				spotter.decode(stream);
			}
			KeywordSpotterResult r = spotter.getResult(stream);
			if (r == null || r.getKeyword() == null || r.getKeyword().isEmpty()) {
				return Collections.emptyList();
			}
			List<KwsResult> out = new ArrayList<>();
			List<String> tokens = new ArrayList<>();
			if (r.getTokens() != null) {
				Collections.addAll(tokens, r.getTokens());
			}
			out.add(KwsResult.builder()
				.keyword(r.getKeyword())
				.tokens(tokens)
				.timestamps(r.getTimestamps())
				.triggeredAtSample(0L)
				.build());
			return out;
		} catch (Throwable t) {
			throw new EngineException("关键词识别失败: " + t.getMessage(), t);
		} finally {
			stream.release();
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("KeywordSpotterService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				spotter.release();
			} catch (Throwable t) {
				log.warn("关闭 KeywordSpotter 失败: {}", t.getMessage());
			}
		}
	}

	/**
	 * 在指定目录下查找第一个存在的文件。
	 */
	private static final class ModelFileUtil {
		private ModelFileUtil() {
		}

		static String firstExisting(String dir, String... candidates) {
			for (String n : candidates) {
				File f = new File(dir, n);
				if (f.isFile()) {
					return f.getAbsolutePath();
				}
			}
			return null;
		}
	}
}
