package net.dreamlu.mica.voice.speaker;

import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor;
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig;
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingManager;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.config.SpeakerConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sherpa-onnx 声纹识别 mica-voice 适配（基于 {@code SpeakerEmbeddingExtractor} + {@code SpeakerEmbeddingManager}）。
 *
 * <p>线程模型：{@code SpeakerEmbeddingManager} 内部持有 native 资源，
 * 对它的 mutation 操作（add / remove）走 synchronized 保护；
 * verify / search 走无锁快照（manager.search 本身线程安全）。
 *
 * @author dreamlu
 */
@Slf4j
public class SpeakerEmbeddingService implements SpeakerService {

	private final MicaVoiceConfig props;
	private final SpeakerConfig config;
	private final SpeakerEmbeddingExtractor extractor;
	private final SpeakerEmbeddingManager manager;
	private final int dim;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public SpeakerEmbeddingService(MicaVoiceConfig props, SpeakerConfig config) {
		this.props = props;
		this.config = config;

		String modelPath = null;
		for (String name : config.getModelCandidates()) {
			String path = ModelSelector.tryResolveModelFile(props.getModelsDir(), name);
			if (path == null) {
				// 也尝试作为文件名直接放在 models 根目录
				File f = new File(props.getModelsDir(), name);
				if (f.isFile()) {
					path = f.getAbsolutePath();
				}
			}
			if (path != null) {
				modelPath = path;
				break;
			}
		}
		if (modelPath == null) {
			throw new EngineException("找不到声纹模型（候选: " + String.join(", ", config.getModelCandidates()) + "），"
				+ "请先下载到 models/ 目录");
		}

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		SpeakerEmbeddingExtractorConfig cfg = SpeakerEmbeddingExtractorConfig.builder()
			.setModel(modelPath)
			.setNumThreads(threads)
			.setDebug(debug)
			.build();

		try {
			this.extractor = new SpeakerEmbeddingExtractor(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 SpeakerEmbeddingExtractor 失败（model=" + modelPath + "）", t);
		}
		this.dim = extractor.getDim();
		this.manager = new SpeakerEmbeddingManager(dim);
		log.info("SpeakerEmbeddingService 初始化完成: model={}, dim={}, threads={}",
			modelPath, dim, threads);
	}

	@Override
	public synchronized SpeakerProfile enroll(String name, File wav) {
		return enroll(name, AudioReaders.read(wav));
	}

	@Override
	public synchronized SpeakerProfile enroll(String name, AudioData audio) {
		ensureOpen();
		float[] embedding = extractEmbedding(audio);
		boolean added = manager.add(name, embedding);
		if (!added) {
			throw new EngineException("注册说话人失败（可能名称已存在）: " + name);
		}
		log.info("注册说话人: {} (dim={})", name, embedding.length);
		return new SpeakerProfile(name, embedding);
	}

	@Override
	public VerificationResult verify(String name, File wav) {
		return verify(name, AudioReaders.read(wav));
	}

	@Override
	public VerificationResult verify(String name, AudioData audio) {
		ensureOpen();
		float[] embedding = extractEmbedding(audio);
		// sherpa-onnx 1.13.5 的 SpeakerEmbeddingManager 不暴露 score 函数，
		// 只暴露 verify(返回 boolean)。这里用 contains + verify 组合判断。
		if (!manager.contains(name)) {
			return new VerificationResult(name, 0f, false, config.getThreshold());
		}
		boolean ok = manager.verify(name, embedding, config.getThreshold());
		return new VerificationResult(name, ok ? 1f : 0f, ok, config.getThreshold());
	}

	@Override
	public SearchResult search(File wav) {
		return search(AudioReaders.read(wav));
	}

	@Override
	public SearchResult search(AudioData audio) {
		ensureOpen();
		float[] embedding = extractEmbedding(audio);
		String name = manager.search(embedding, config.getThreshold());
		// sherpa-onnx 的 search 无匹配时返回空字符串（而非 null），统一视为未命中
		if (name == null || name.isEmpty()) {
			return SearchResult.empty(config.getThreshold());
		}
		return new SearchResult(name, 1f, config.getThreshold());
	}

	@Override
	public synchronized List<String> names() {
		ensureOpen();
		String[] all = manager.getAllSpeakerNames();
		List<String> list = new ArrayList<>(all == null ? 0 : all.length);
		if (all != null) {
			java.util.Collections.addAll(list, all);
		}
		return list;
	}

	@Override
	public int size() {
		ensureOpen();
		return manager.getNumSpeakers();
	}

	@Override
	public synchronized boolean remove(String name) {
		ensureOpen();
		return manager.remove(name);
	}

	/**
	 * 从音频中提取嵌入向量。
	 */
	private float[] extractEmbedding(AudioData audio) {
		OnlineStream stream = extractor.createStream();
		try {
			stream.acceptWaveform(audio.getSamples(), audio.getSampleRate());
			stream.inputFinished();
			long waitMs = 0;
			long stepMs = 50;
			long timeout = config.getEmbeddingTimeoutMs() <= 0 ? 30_000L : config.getEmbeddingTimeoutMs();
			while (!extractor.isReady(stream)) {
				if (waitMs > timeout) {
					throw new EngineException("提取说话人嵌入超时（" + timeout + "ms）");
				}
				try {
					Thread.sleep(stepMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new EngineException("提取说话人嵌入被中断", e);
				}
				waitMs += stepMs;
			}
			return extractor.compute(stream);
		} finally {
			stream.release();
		}
	}

	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("SpeakerEmbeddingService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				manager.release();
			} catch (Throwable t) {
				log.warn("关闭 SpeakerEmbeddingManager 失败: {}", t.getMessage());
			}
			try {
				extractor.release();
			} catch (Throwable t) {
				log.warn("关闭 SpeakerEmbeddingExtractor 失败: {}", t.getMessage());
			}
		}
	}
}
