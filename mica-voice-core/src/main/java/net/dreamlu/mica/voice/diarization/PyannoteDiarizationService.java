package net.dreamlu.mica.voice.diarization;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.config.DiarizationConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx OfflineSpeakerDiarization 的 mica-voice 适配（默认 Pyannote 分割）。
 *
 * <p>模型三件套：
 * <ul>
 *     <li>segmentation：sherpa-onnx-pyannote-segmentation-3-0.onnx</li>
 *     <li>embedding：3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx</li>
 *     <li>clustering：FastClusteringConfig（无需模型）</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
public class PyannoteDiarizationService implements DiarizationService {

	private final MicaVoiceConfig props;
	private final DiarizationConfig config;
	private final OfflineSpeakerDiarization diarization;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public PyannoteDiarizationService(MicaVoiceConfig props, DiarizationConfig config) {
		this.props = props;
		this.config = config;

		String segmentationPath = resolveModel(config.getSegmentationModelFileName());
		String embeddingPath = resolveModel(config.getEmbeddingModelFileName());

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OfflineSpeakerSegmentationPyannoteModelConfig pyannoteCfg =
			OfflineSpeakerSegmentationPyannoteModelConfig.builder()
				.setModel(segmentationPath)
				.build();
		OfflineSpeakerSegmentationModelConfig segmentation =
			OfflineSpeakerSegmentationModelConfig.builder()
				.setPyannote(pyannoteCfg)
				.setNumThreads(threads)
				.setDebug(debug)
				.build();

		SpeakerEmbeddingExtractorConfig embedding = SpeakerEmbeddingExtractorConfig.builder()
			.setModel(embeddingPath)
			.setNumThreads(threads)
			.setDebug(debug)
			.build();

		FastClusteringConfig clustering = FastClusteringConfig.builder()
			.setNumClusters(config.getNumClusters())
			.setThreshold(config.getClusterThreshold())
			.build();

		OfflineSpeakerDiarizationConfig cfg = OfflineSpeakerDiarizationConfig.builder()
			.setSegmentation(segmentation)
			.setEmbedding(embedding)
			.setClustering(clustering)
			.setMinDurationOff(config.getMinDurationOff())
			.setMinDurationOn(config.getMinDurationOn())
			.build();

		try {
			this.diarization = new OfflineSpeakerDiarization(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 SpeakerDiarization 失败（segmentation=" + segmentationPath
				+ ", embedding=" + embeddingPath + "）", t);
		}
		log.info("SpeakerDiarization 初始化完成: segmentation={}, embedding={}", segmentationPath, embeddingPath);
	}

	private String resolveModel(String fileName) {
		File direct = new File(props.getModelsDir(), fileName);
		if (direct.isFile()) {
			return direct.getAbsolutePath();
		}
		File modelsDir = props.getModelsDir();
		if (modelsDir != null && modelsDir.isDirectory()) {
			File[] found = modelsDir.listFiles((d, n) -> n.equals(fileName));
			if (found != null && found.length > 0) {
				return found[0].getAbsolutePath();
			}
		}
		throw new EngineException("找不到模型: " + fileName + "（请放到 " + modelsDir + " 下）");
	}

	@Override
	public List<DiarizationSegment> diarize(AudioData audio) {
		ensureOpen();
		try {
			OfflineSpeakerDiarizationSegment[] raw = diarization.process(audio.getSamples());
			List<DiarizationSegment> out = new ArrayList<>(raw == null ? 0 : raw.length);
			if (raw != null) {
				for (OfflineSpeakerDiarizationSegment s : raw) {
					out.add(DiarizationSegment.builder()
						.startSec(s.getStart())
						.endSec(s.getEnd())
						.speaker(s.getSpeaker())
						.build());
				}
			}
			return out;
		} catch (Throwable t) {
			throw new EngineException("说话人分离失败: " + t.getMessage(), t);
		}
	}

	/**
	 * 暴露底层 {@link OfflineSpeakerDiarization} 给高级用法（如联合 ASR）。
	 */
	public OfflineSpeakerDiarization getDiarization() {
		ensureOpen();
		return diarization;
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("PyannoteDiarizationService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				diarization.release();
			} catch (Throwable t) {
				log.warn("关闭 SpeakerDiarization 失败: {}", t.getMessage());
			}
		}
	}
}
