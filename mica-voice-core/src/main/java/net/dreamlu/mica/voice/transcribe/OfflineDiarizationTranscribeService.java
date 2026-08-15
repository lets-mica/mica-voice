package net.dreamlu.mica.voice.transcribe;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.diarization.DiarizationSegment;
import net.dreamlu.mica.voice.diarization.DiarizationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "说话人分离 + 转写"联合服务。
 *
 * <p>策略：
 * <ol>
 *     <li>用 {@link DiarizationService} 把音频切成 N 段（speaker + 时间区间）</li>
 *     <li>对每段音频切片调 {@link AsrService} 识别</li>
 *     <li>合并成 {@link TranscribeResult} 输出</li>
 * </ol>
 *
 * <p>注意：当前实现采用简单的时间窗口切分，要求 diarization 与 ASR 都使用同一采样率
 * （推荐 16kHz）。重叠区间会落到第一个匹配的 segment。
 *
 * @author dreamlu
 */
@Slf4j
public class OfflineDiarizationTranscribeService {

	private final DiarizationService diarization;
	private final AsrService asr;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/**
	 * 构造"说话人分离 + 转写"联合服务。
	 *
	 * @param diarization 说话人分离服务
	 * @param asr         ASR 服务
	 */
	public OfflineDiarizationTranscribeService(DiarizationService diarization, AsrService asr) {
		if (diarization == null) {
			throw new IllegalArgumentException("DiarizationService 不能为空");
		}
		if (asr == null) {
			throw new IllegalArgumentException("AsrService 不能为空");
		}
		this.diarization = diarization;
		this.asr = asr;
	}

	/**
	 * 联合分析：分离 + 转写。
	 *
	 * @param audio 待分析的完整音频
	 * @return 转写结果（含每段对应的说话人 id 与文本）
	 */
	public TranscribeResult transcribe(AudioData audio) {
		ensureOpen();
		long start = System.currentTimeMillis();
		// 1. 说话人分离
		List<DiarizationSegment> segments = diarization.diarize(audio);
		if (segments.isEmpty()) {
			log.warn("说话人分离未检测到任何说话人片段");
			return TranscribeResult.builder()
				.segments(new ArrayList<>())
				.numSpeakers(0)
				.costMs(System.currentTimeMillis() - start)
				.build();
		}

		// 2. 收集 unique speaker id
		Set<Integer> speakerSet = new HashSet<>();
		for (DiarizationSegment s : segments) {
			speakerSet.add(s.getSpeaker());
		}

		// 3. 对每段切片做 ASR
		float[] samples = audio.getSamples();
		int sampleRate = audio.getSampleRate();
		List<TranscribedSegment> transcribed = new ArrayList<>(segments.size());
		for (DiarizationSegment seg : segments) {
			int startSample = Math.max(0, (int) Math.round(seg.getStartSec() * sampleRate));
			int endSample = Math.min(samples.length, (int) Math.round(seg.getEndSec() * sampleRate));
			if (endSample <= startSample) {
				// 段太短或越界，跳过
				continue;
			}
			int len = endSample - startSample;
			float[] chunk = new float[len];
			System.arraycopy(samples, startSample, chunk, 0, len);
			AudioData chunkAudio = new AudioData(chunk, sampleRate);
			try {
				AsrResult result = asr.recognize(chunkAudio);
				transcribed.add(TranscribedSegment.builder()
					.speaker(seg.getSpeaker())
					.startMs((long) (seg.getStartSec() * 1000L))
					.endMs((long) (seg.getEndSec() * 1000L))
					.text(result.getText() == null ? "" : result.getText().trim())
					.build());
			} catch (Throwable t) {
				log.warn("说话人 [{}] 段 [{}ms - {}ms] ASR 失败: {}",
					seg.getSpeaker(),
					(long) (seg.getStartSec() * 1000L),
					(long) (seg.getEndSec() * 1000L),
					t.getMessage());
				transcribed.add(TranscribedSegment.builder()
					.speaker(seg.getSpeaker())
					.startMs((long) (seg.getStartSec() * 1000L))
					.endMs((long) (seg.getEndSec() * 1000L))
					.text("")
					.build());
			}
		}

		long cost = System.currentTimeMillis() - start;
		return TranscribeResult.builder()
			.segments(transcribed)
			.numSpeakers(speakerSet.size())
			.costMs(cost)
			.build();
	}

	/**
	 * 关闭服务：级联关闭 {@link DiarizationService} 与 {@link AsrService}。重复调用安全。
	 */
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				diarization.close();
			} catch (Throwable t) {
				log.warn("关闭 DiarizationService 失败: {}", t.getMessage());
			}
			try {
				asr.close();
			} catch (Throwable t) {
				log.warn("关闭 AsrService 失败: {}", t.getMessage());
			}
		}
	}

	/**
	 * 确保服务未被关闭，否则抛出 IllegalStateException。
	 */
	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OfflineDiarizationTranscribeService 已关闭");
		}
	}
}
