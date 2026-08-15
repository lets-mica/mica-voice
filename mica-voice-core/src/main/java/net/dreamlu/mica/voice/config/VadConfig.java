package net.dreamlu.mica.voice.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VAD（语音活动检测）配置。
 *
 * <p>支持两种 VAD 模型：
 * <ul>
 *     <li>SILERO VAD：silero_vad.onnx（默认，~1MB，CPU 友好）</li>
 *     <li>TEN VAD：ten_vad.onnx</li>
 * </ul>
 *
 * @author dreamlu
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VadConfig {

	/**
	 * 默认 SILERO VAD 模型名。
	 */
	public static final String[] DEFAULT_MODEL_CANDIDATES = new String[]{
		"silero_vad.onnx"
	};
	@Builder.Default
	private String modelFileName = "silero_vad.onnx";
	@Builder.Default
	private ModelType modelType = ModelType.SILERO;
	/**
	 * 模型要求的采样率；SILERO VAD 推荐 16000。
	 */
	@Builder.Default
	private int sampleRate = 16000;
	private Integer threads;
	private boolean debug;
	/**
	 * 触发语音的阈值（越高越严格）
	 */
	@Builder.Default
	private float threshold = 0.5f;
	/**
	 * 最小静音时长（秒），超过判定为一句话结束
	 */
	@Builder.Default
	private float minSilenceDuration = 0.5f;
	/**
	 * 最小语音时长（秒），短于此视为噪声
	 */
	@Builder.Default
	private float minSpeechDuration = 0.25f;
	/**
	 * 语音最大时长（秒），超过则强制切分
	 */
	@Builder.Default
	private float maxSpeechDuration = 20.0f;
	/**
	 * SILERO VAD 窗口大小（512 / 1024 / 1536 samples）
	 */
	@Builder.Default
	private int windowSize = 512;

	public enum ModelType {
		SILERO,
		TEN
	}
}