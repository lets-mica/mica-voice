package net.dreamlu.mica.voice.tts;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * TTS 合成结果（不可变）。
 *
 * <p>采样取值范围：{@code [-1.0f, 1.0f]}；采样率由模型决定（如 VITS-aishell3 = 22050Hz）。
 *
 * @author dreamlu
 */
@Value
@Builder
public class TtsAudio {

	float[] samples;
	int sampleRate;
	int speakerId;
	float speed;
	long costMs;

	public TtsAudio(float[] samples, int sampleRate, int speakerId, float speed, long costMs) {
		Objects.requireNonNull(samples, "samples");
		if (sampleRate <= 0) {
			throw new IllegalArgumentException("sampleRate must be > 0");
		}
		this.samples = samples;
		this.sampleRate = sampleRate;
		this.speakerId = speakerId;
		this.speed = speed;
		this.costMs = costMs;
	}

	/**
	 * 时长（秒）
	 */
	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
