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

	/**
	 * TTS 合成得到的样本（取值范围 {@code [-1.0f, 1.0f]}）。
	 */
	float[] samples;
	/**
	 * 合成结果采样率（Hz），由模型决定（如 VITS-aishell3 = 22050Hz）。
	 */
	int sampleRate;
	/**
	 * 本次合成使用的说话人 id。
	 */
	int speakerId;
	/**
	 * 本次合成使用的语速（1.0 = 默认）。
	 */
	float speed;
	/**
	 * 本次合成耗时（毫秒）。
	 */
	long costMs;

	/**
	 * 构造一份 TTS 合成结果。
	 *
	 * @param samples    样本
	 * @param sampleRate 采样率（必须 &gt; 0）
	 * @param speakerId  说话人 id
	 * @param speed      语速
	 * @param costMs     耗时（毫秒）
	 */
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
	 * 时长（秒）。
	 *
	 * @return 音频时长（秒）
	 */
	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
