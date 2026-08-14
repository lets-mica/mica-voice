package net.dreamlu.mica.voice.denoise;

import com.k2fsa.sherpa.onnx.*;
import com.k2fsa.sherpa.onnx.DenoisedAudio;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.config.DenoiseConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx OfflineSpeechDenoiser 的 mica-voice 适配（GTCRN / DPDFNet）。
 *
 * @author dreamlu
 */
@Slf4j
public class OfflineSpeechDenoiserService implements DenoiseService {

	private final MicaVoiceConfig props;
	private final DenoiseConfig config;
	private final OfflineSpeechDenoiser denoiser;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public OfflineSpeechDenoiserService(MicaVoiceConfig props, DenoiseConfig config) {
		this.props = props;
		this.config = config;

		String modelPath = resolveModel();
		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OfflineSpeechDenoiserModelConfig.Builder modelBuilder = OfflineSpeechDenoiserModelConfig.builder()
			.setNumThreads(threads)
			.setDebug(debug);
		switch (config.getModelType()) {
			case GTCRN:
				modelBuilder.setGtcrn(OfflineSpeechDenoiserGtcrnModelConfig.builder()
					.setModel(modelPath)
					.build());
				break;
			case DPDFNet:
				modelBuilder.setDpdfnet(OfflineSpeechDenoiserDpdfNetModelConfig.builder()
					.setModel(modelPath)
					.setAttenuationLimitDb(config.getAttenuationLimitDb())
					.build());
				break;
			default:
				modelBuilder.setGtcrn(OfflineSpeechDenoiserGtcrnModelConfig.builder()
					.setModel(modelPath).build());
		}

		OfflineSpeechDenoiserConfig cfg = OfflineSpeechDenoiserConfig.builder()
			.setModel(modelBuilder.build())
			.build();

		try {
			this.denoiser = new OfflineSpeechDenoiser(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 OfflineSpeechDenoiser 失败（model=" + modelPath + "）", t);
		}
		log.info("OfflineSpeechDenoiser 初始化完成: model={}, type={}", modelPath, config.getModelType());
	}

	private String resolveModel() {
		String name = config.getModelFileName();
		File direct = new File(props.getModelsDir(), name);
		if (direct.isFile()) {
			return direct.getAbsolutePath();
		}
		File modelsDir = props.getModelsDir();
		if (modelsDir != null && modelsDir.isDirectory()) {
			File[] found = modelsDir.listFiles((d, n) -> n.equals(name));
			if (found != null && found.length > 0) {
				return found[0].getAbsolutePath();
			}
		}
		throw new EngineException("找不到模型: " + name + "（请放到 " + modelsDir + " 下）");
	}

	@Override
	public net.dreamlu.mica.voice.denoise.DenoisedAudio denoise(AudioData audio) {
		ensureOpen();
		long start = System.currentTimeMillis();
		try {
			DenoisedAudio raw = denoiser.run(audio.getSamples(), audio.getSampleRate());
			long cost = System.currentTimeMillis() - start;
			return net.dreamlu.mica.voice.denoise.DenoisedAudio.builder()
				.samples(raw.getSamples())
				.sampleRate(raw.getSampleRate())
				.costMs(cost)
				.build();
		} catch (Throwable t) {
			throw new EngineException("降噪失败: " + t.getMessage(), t);
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OfflineSpeechDenoiserService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				denoiser.release();
			} catch (Throwable t) {
				log.warn("关闭 OfflineSpeechDenoiser 失败: {}", t.getMessage());
			}
		}
	}
}
