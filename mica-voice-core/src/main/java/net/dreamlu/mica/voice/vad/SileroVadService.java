package net.dreamlu.mica.voice.vad;

import com.k2fsa.sherpa.onnx.SileroVadModelConfig;
import com.k2fsa.sherpa.onnx.SpeechSegment;
import com.k2fsa.sherpa.onnx.Vad;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.config.VadConfig;
import net.dreamlu.mica.voice.exception.EngineException;
import net.dreamlu.mica.voice.exception.ModelNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx VAD mica-voice 适配（默认基于 SILERO VAD）。
 *
 * <p>支持 SILERO 与 TEN 两种 VAD 模型族（通过 {@link VadConfig.ModelType} 选择）。
 *
 * @author dreamlu
 */
@Slf4j
public class SileroVadService implements VadService {

	private final MicaVoiceConfig props;
	private final VadConfig config;
	private final Vad vad;
	private final int sampleRate;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public SileroVadService(MicaVoiceConfig props, VadConfig config) {
		this.props = props;
		this.config = config;
		this.sampleRate = config.getSampleRate();

		String modelPath = resolveModel();
		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		SileroVadModelConfig.Builder silero = SileroVadModelConfig.builder()
			.setModel(modelPath)
			.setThreshold(config.getThreshold())
			.setMinSilenceDuration(config.getMinSilenceDuration())
			.setMinSpeechDuration(config.getMinSpeechDuration())
			.setWindowSize(config.getWindowSize())
			.setMaxSpeechDuration(config.getMaxSpeechDuration());

		VadModelConfig.Builder modelBuilder = VadModelConfig.builder()
			.setSampleRate(sampleRate)
			.setNumThreads(threads)
			.setDebug(debug);
		switch (config.getModelType()) {
			case SILERO:
				modelBuilder.setSileroVadModelConfig(silero.build());
				break;
			case TEN:
				// TEN VAD 沿用同一份 silero builder 风格（实际类不同：TenVadModelConfig）
				com.k2fsa.sherpa.onnx.TenVadModelConfig ten =
					com.k2fsa.sherpa.onnx.TenVadModelConfig.builder()
						.setModel(modelPath)
						.setThreshold(config.getThreshold())
						.setMinSilenceDuration(config.getMinSilenceDuration())
						.setMinSpeechDuration(config.getMinSpeechDuration())
						.setWindowSize(config.getWindowSize())
						.setMaxSpeechDuration(config.getMaxSpeechDuration())
						.build();
				modelBuilder.setTenVadModelConfig(ten);
				break;
			default:
				modelBuilder.setSileroVadModelConfig(silero.build());
		}

		try {
			this.vad = new Vad(modelBuilder.build());
		} catch (Throwable t) {
			throw new EngineException("初始化 VAD 失败（model=" + modelPath + "）", t);
		}
		log.info("VAD 初始化完成: model={}, sampleRate={}, threads={}, type={}",
			modelPath, sampleRate, threads, config.getModelType());
	}

	private String resolveModel() {
		String name = config.getModelFileName();
		// 1. 在 modelsDir 根目录直接查找
		File direct = new File(props.getModelsDir(), name);
		if (direct.isFile()) {
			return direct.getAbsolutePath();
		}
		// 2. 在 modelsDir 下任意子目录查找
		File modelsDir = props.getModelsDir();
		if (modelsDir != null && modelsDir.isDirectory()) {
			File[] found = modelsDir.listFiles((d, n) -> n.equals(name));
			if (found != null && found.length > 0) {
				return found[0].getAbsolutePath();
			}
		}
		// 3. 兜底用 ModelSelector 抛 ModelNotFoundException
		ModelSelector.resolveModelFile(props.getModelsDir(), name);
		throw new ModelNotFoundException(name, new String[]{name});
	}

	@Override
	public List<VadSegment> detect(AudioData audio) {
		ensureOpen();
		if (audio.getSampleRate() != sampleRate) {
			// 不强转 resample（Phase 1 不做），由上层负责重采样
			log.warn("VAD 期望 {}Hz 采样率，实际 {}Hz，建议先用 ffmpeg 重采样",
				sampleRate, audio.getSampleRate());
		}
		vad.acceptWaveform(audio.getSamples());
		vad.flush();
		List<VadSegment> out = new ArrayList<>();
		while (!vad.empty()) {
			SpeechSegment seg = vad.front();
			out.add(VadSegment.builder()
				.startSample(seg.getStart())
				.samples(seg.getSamples())
				.sampleRate(sampleRate)
				.build());
			vad.pop();
		}
		vad.clear();
		return out;
	}

	@Override
	public void acceptWaveform(float[] samples) {
		ensureOpen();
		vad.acceptWaveform(samples);
	}

	@Override
	public List<VadSegment> poll() {
		ensureOpen();
		if (vad.empty()) {
			return Collections.emptyList();
		}
		List<VadSegment> out = new ArrayList<>();
		while (!vad.empty()) {
			SpeechSegment seg = vad.front();
			out.add(VadSegment.builder()
				.startSample(seg.getStart())
				.samples(seg.getSamples())
				.sampleRate(sampleRate)
				.build());
			vad.pop();
		}
		return out;
	}

	@Override
	public boolean isSpeechDetected() {
		ensureOpen();
		return vad.isSpeechDetected();
	}

	@Override
	public void flush() {
		ensureOpen();
		vad.flush();
	}

	@Override
	public void reset() {
		ensureOpen();
		vad.reset();
		vad.clear();
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("SileroVadService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				vad.release();
			} catch (Throwable t) {
				log.warn("关闭 VAD 失败: {}", t.getMessage());
			}
		}
	}
}
