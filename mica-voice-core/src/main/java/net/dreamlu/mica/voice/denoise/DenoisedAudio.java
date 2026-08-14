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

	float[] samples;
	int sampleRate;
	long costMs;

	public float durationSeconds() {
		return samples.length / (float) sampleRate;
	}
}
