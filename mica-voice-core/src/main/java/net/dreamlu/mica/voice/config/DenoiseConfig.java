package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 音频降噪（Denoise）配置。
 *
 * <p>支持两种 sherpa-onnx 模型：
 * <ul>
 *     <li>GTCRN：轻量级流式降噪</li>
 *     <li>DeepFilterNet（dfpdfnet）：高质量离线降噪</li>
 * </ul>
 *
 * @author dreamlu
 */
@Getter
@Setter
public class DenoiseConfig {

	private String modelFileName = "sherpa-onnx-gtcrn.onnx";
	private ModelType modelType = ModelType.GTCRN;
	private Integer threads;
	private boolean debug;
	/**
	 * 仅 DPDFNet：衰减限制（dB），控制降噪强度。
	 */
	private float attenuationLimitDb = 12.0f;

	public DenoiseConfig() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public enum ModelType {
		GTCRN,
		DPDFNet
	}

	public static final class Builder {
		private final DenoiseConfig c = new DenoiseConfig();

		public Builder modelFileName(String n) {
			c.modelFileName = n;
			return this;
		}

		public Builder modelType(ModelType t) {
			c.modelType = t == null ? ModelType.GTCRN : t;
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

		public Builder attenuationLimitDb(float v) {
			c.attenuationLimitDb = v;
			return this;
		}

		public DenoiseConfig build() {
			return c;
		}
	}
}
