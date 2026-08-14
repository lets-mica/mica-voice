package net.dreamlu.mica.voice.diarization;

import lombok.Builder;
import lombok.Value;

/**
 * 一段被分配了说话人 id 的音频片段（不可变）。
 *
 * <p>时间单位：<b>秒</b>。
 *
 * @author dreamlu
 */
@Value
@Builder
public class DiarizationSegment {

	/**
	 * 起始时间（秒）。
	 */
	float startSec;
	/**
	 * 结束时间（秒）。
	 */
	float endSec;
	/**
	 * 说话人 id（从 0 开始）
	 */
	int speaker;
}
