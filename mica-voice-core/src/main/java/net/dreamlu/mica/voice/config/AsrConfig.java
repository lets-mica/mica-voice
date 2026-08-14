package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 离线 ASR 配置。
 *
 * <p>支持任意 sherpa-onnx OfflineRecognizer 模型家族：Paraformer / Whisper /
 * SenseVoice / Zipformer / Moonshine 等，只要模型目录里有 {@code model.onnx}
 * 与 {@code tokens.txt} 即可。模型家族由 {@link #modelType} 决定。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class AsrConfig {

	private String modelDirName;
	private ModelType modelType = ModelType.PARAFORMER;
	private Integer threads;
	private boolean debug;
	/**
	 * SenseVoice 专用：auto/zh/en/ja/ko/yue
	 */
	private String language = "auto";
	/**
	 * SenseVoice 专用：是否做逆文本规范化（数字/标点还原）
	 */
	private boolean inverseTextNormalization = true;

	public AsrConfig() {
	}

	public AsrConfig(String modelDirName) {
		this.modelDirName = modelDirName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType == null ? ModelType.AUTO : modelType;
	}

	/**
	 * 模型家族枚举，对应 sherpa-onnx 的 Offline*ModelConfig。
	 */
	public enum ModelType {
		/**
		 * Paraformer（非自回归 Transformer）
		 */
		PARAFORMER,
		/**
		 * SenseVoice（多语言 + 情感 + 事件）
		 */
		SENSE_VOICE,
		/**
		 * Whisper（多语言）
		 */
		WHISPER,
		/**
		 * Moonshine（轻量 Whisper 替代）
		 */
		MOONSHINE,
		/**
		 * Zipformer（k2-fsa 自研）
		 */
		ZIPFORMER,
		/**
		 * NeMo CTC
		 */
		NEMO_CTC,
		/**
		 * 通用：只设置 model + tokens，由 sherpa-onnx 自动推断
		 */
		AUTO
	}

	public static final class Builder {
		private final AsrConfig c = new AsrConfig();

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

		public Builder language(String l) {
			c.language = l;
			return this;
		}

		public Builder inverseTextNormalization(boolean b) {
			c.inverseTextNormalization = b;
			return this;
		}

		public AsrConfig build() {
			return c;
		}
	}
}
