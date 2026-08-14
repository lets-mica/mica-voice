package net.dreamlu.mica.voice.transcribe;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 整段音频的"说话人分离 + 转写"结果（不可变）。
 *
 * <p>每条 {@link TranscribedSegment} 包含某个 speaker 在某段时间说的文本。
 *
 * @author dreamlu
 */
@Value
@Builder
public class TranscribeResult {

	/**
	 * 转写片段（按时间排序）
	 */
	List<TranscribedSegment> segments;
	/**
	 * 说话人总数
	 */
	int numSpeakers;
	/**
	 * 总耗时（毫秒）
	 */
	long costMs;
}
