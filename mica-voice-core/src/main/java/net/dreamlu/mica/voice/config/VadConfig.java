package net.dreamlu.mica.voice.config;

import lombok.Getter;
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
public class VadConfig {

	/**
	 * 默认 SILERO VAD 模型名。
	 */
	public static final String[] DEFAULT_MODEL_CANDIDATES = new String[]{
		"silero_vad.onnx"
	};
	private String modelFileName = "silero_vad.onnx";
	private ModelType modelType = ModelType.SILERO;
	/**
	 * 模型要求的采样率；SILERO VAD 推荐 16000。
	 */
	private int sampleRate = 16000;
	private Integer threads;
	private boolean debug;
	/**
	 * 触发语音的阈值（越高越严格）
	 */
	private float threshold = 0.5f;
	/**
	 * 最小静音时长（秒），超过判定为一句话结束
	 */
	private float minSilenceDuration = 0.5f;
	/**
	 * 最小语音时长（秒），短于此视为噪声
	 */
	private float minSpeechDuration = 0.25f;
	/**
	 * 语音最大时长（秒），超过则强制切分
	 */
	private float maxSpeechDuration = 20.0f;
	/**
	 * SILERO VAD 窗口大小（512 / 1024 / 1536 samples）
	 */
	private int windowSize = 512;
	public VadConfig() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public enum ModelType {
		SILERO,
		TEN
	}

	public static final class Builder {
		private final VadConfig c = new VadConfig();

		public Builder modelFileName(String n) {
			c.modelFileName = n;
			return this;
		}

		public Builder modelType(ModelType t) {
			c.modelType = t == null ? ModelType.SILERO : t;
			return this;
		}

		public Builder sampleRate(int v) {
			c.sampleRate = v;
			return this;
		}

		public Builder threads(Integer t) {
			c.threads = t;
			return this;
		}

		public Builder debug(boolean d) {
			c.debug = d;
			return this;
		}

		public Builder threshold(float v) {
			c.threshold = v;
			return this;
		}

		public Builder minSilenceDuration(float v) {
			c.minSilenceDuration = v;
			return this;
		}

		public Builder minSpeechDuration(float v) {
			c.minSpeechDuration = v;
			return this;
		}

		public Builder maxSpeechDuration(float v) {
			c.maxSpeechDuration = v;
			return this;
		}

		public Builder windowSize(int v) {
			c.windowSize = v;
			return this;
		}

		public VadConfig build() {
			return c;
		}
	}
}
