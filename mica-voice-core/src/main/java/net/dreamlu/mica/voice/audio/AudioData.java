package net.dreamlu.mica.voice.audio;

import lombok.Value;

import java.util.Objects;

/**
 * 统一的音频数据结构。
 *
 * <p>Phase 1 仅承担"采样率 + 单声道 float[] 样本"的轻量包装；
 * 多声道 / 24-bit / mp3 等复杂格式的解码留给上层（如 ffmpeg）。
 *
 * <p>样本取值范围：{@code [-1.0f, 1.0f]}。
 *
 * @author dreamlu
 */
@Value
public class AudioData {

	float[] samples;
	int sampleRate;

	public AudioData(float[] samples, int sampleRate) {
		Objects.requireNonNull(samples, "samples");
		if (sampleRate <= 0) {
			throw new IllegalArgumentException("sampleRate must be > 0, got " + sampleRate);
		}
		this.samples = samples;
		this.sampleRate = sampleRate;
	}

	/**
	 * 时长（秒）
	 */
	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
