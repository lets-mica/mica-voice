package net.dreamlu.mica.voice.vad;

import lombok.Builder;
import lombok.Value;

/**
 * 一段被 VAD 判定为语音的音频片段（不可变）。
 *
 * <p>{@code startSample} 是相对输入音频起点的样本偏移；
 * {@code samples} 是该片段对应的样本数据（采样率与输入一致）。
 *
 * @author dreamlu
 */
@Value
@Builder
public class VadSegment {

	/**
	 * 起始样本偏移（samples）。
	 */
	int startSample;
	/**
	 * 片段样本数据。
	 */
	float[] samples;
	/**
	 * 该片段的采样率（与输入一致）。
	 */
	int sampleRate;

	/**
	 * 时长（秒）。
	 *
	 * @return 片段时长（秒）
	 */
	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
