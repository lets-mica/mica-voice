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

	/**
	 * 单声道音频样本（取值范围 {@code [-1.0f, 1.0f]}）。
	 */
	float[] samples;
	/**
	 * 采样率（Hz），例如 16000 / 22050 / 44100。
	 */
	int sampleRate;

	/**
	 * 构造一份音频数据。
	 *
	 * @param samples    单声道 float[] 样本，必须非空
	 * @param sampleRate 采样率，必须大于 0
	 */
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
