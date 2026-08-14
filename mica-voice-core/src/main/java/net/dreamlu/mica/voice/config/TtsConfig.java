package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

/**
 * TTS（语音合成）配置。
 *
 * <p>当前以 VITS 模型为主（轻量、多说话人）。模型目录里应包含
 * {@code model.onnx / lexicon.txt / tokens.txt}；icefall 系还会带 {@code dict/} 子目录，
 * SDK 会自动检测并设置 dictDir / dataDir。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class TtsConfig {

	private String modelDirName;
	private ModelType modelType = ModelType.VITS;
	private Integer threads;
	private boolean debug;
	/**
	 * 默认说话人 id
	 */
	private int defaultSpeakerId = 0;
	/**
	 * 默认语速
	 */
	private float defaultSpeed = 1.0f;
	/**
	 * 回调式合成时，每多少采样回调一次（默认 1600 ≈ 100ms @ 16kHz）
	 */
	private int callbackSampleStep = 1600;

	public TtsConfig() {
	}

	public TtsConfig(String modelDirName) {
		this.modelDirName = modelDirName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType == null ? ModelType.VITS : modelType;
	}

	public enum ModelType {
		VITS,
		MATCHA,
		KOKORO,
		/**
		 * 自动（v1 仅支持 VITS，留作扩展）
		 */
		AUTO
	}

	public static final class Builder {
		private final TtsConfig c = new TtsConfig();

		public Builder modelDirName(String name) {
			c.modelDirName = name;
			return this;
		}

		public Builder modelType(ModelType t) {
			c.modelType = t;
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

		public Builder defaultSpeakerId(int id) {
			c.defaultSpeakerId = id;
			return this;
		}

		public Builder defaultSpeed(float s) {
			c.defaultSpeed = s;
			return this;
		}

		public Builder callbackSampleStep(int n) {
			c.callbackSampleStep = n;
			return this;
		}

		public TtsConfig build() {
			return c;
		}
	}
}
