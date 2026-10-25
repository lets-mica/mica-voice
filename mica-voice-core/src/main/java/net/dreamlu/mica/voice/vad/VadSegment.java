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
	 *
	 * <p>类型为 {@code long}：sherpa-onnx 2.0.0 起把 VAD 样本位置加宽到 int64
	 * （上游 #4079，修复长时音频累计样本数溢出 int 导致的 SIGSEGV）。
	 * 长时音频（&gt; 约 12.4 小时 @16kHz）的偏移量会超出 int 范围，这里必须同步加宽，
	 * 不能强转回 int。
	 */
	long startSample;
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
