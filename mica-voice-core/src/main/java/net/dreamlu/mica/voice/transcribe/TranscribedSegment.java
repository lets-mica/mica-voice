package net.dreamlu.mica.voice.transcribe;

import lombok.Builder;
import lombok.Value;

/**
 * 一段"某个说话人在某段时间说了什么"的转写片段（不可变）。
 *
 * <p>时间单位：<b>毫秒</b>。speaker 是整型编号（0 起），具体姓名由调用方根据场景命名。
 *
 * @author dreamlu
 */
@Value
@Builder
public class TranscribedSegment {

	/**
	 * 说话人 id（0 起）
	 */
	int speaker;
	/**
	 * 起始时间（毫秒）
	 */
	long startMs;
	/**
	 * 结束时间（毫秒）
	 */
	long endMs;
	/**
	 * 该段转写文本
	 */
	String text;
	/**
	 * ASR 置信度（0~1，部分模型可能为 null）
	 */
	Float confidence;

	/**
	 * 时长（毫秒）。
	 *
	 * @return {@code endMs - startMs}
	 */
	public long durationMs() {
		return endMs - startMs;
	}
}
