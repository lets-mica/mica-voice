package net.dreamlu.mica.voice.denoise;

import lombok.Builder;
import lombok.Value;

/**
 * 降噪后的音频（不可变）。
 *
 * @author dreamlu
 */
@Value
@Builder
public class DenoisedAudio {

	/**
	 * 降噪后样本（取值范围 {@code [-1.0f, 1.0f]}）。
	 */
	float[] samples;
	/**
	 * 降噪后采样率（Hz）。
	 */
	int sampleRate;
	/**
	 * 本次降噪耗时（毫秒）。
	 */
	long costMs;

	/**
	 * 时长（秒）。
	 *
	 * @return 音频时长（秒）
	 */
	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
