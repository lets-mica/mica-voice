package net.dreamlu.mica.voice.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 声纹识别配置。
 *
 * <p>需要 {@code *.onnx} 嵌入模型（典型为 3D-Speaker / eres2net 系列），
 * 文件可直接放在 {@code models/} 根下，也可放在子目录里——SDK 会按候选名查找。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class SpeakerConfig {

	/**
	 * 模型候选名（按优先级查找）。
	 * 用户可在 {@code models/} 下放任意一个，SDK 会自动选第一个存在的。
	 */
	public static final String[] DEFAULT_MODEL_CANDIDATES = new String[]{
		"3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
		"3dspeaker_speech_eres2net_base_sv_zh_en.onnx",
		"3dspeaker_speech_campplus_sv_zh_en.onnx"
	};

	@Getter(AccessLevel.NONE)
	private String[] modelCandidates;
	private Integer threads;
	private boolean debug;
	/**
	 * 相似度阈值（cosine），超过判定为同一人
	 */
	private float threshold = 0.5f;
	/**
	 * 提取嵌入时，等待特征就绪的最大超时（毫秒）
	 */
	private long embeddingTimeoutMs = 30_000L;

	public SpeakerConfig() {
		this.modelCandidates = DEFAULT_MODEL_CANDIDATES.clone();
	}

	/**
	 * 创建一个流式 Builder。
	 *
	 * @return 配置 Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public void setModelCandidates(String[] modelCandidates) {
		this.modelCandidates = modelCandidates == null ? new String[0] : modelCandidates.clone();
	}

	public String[] getModelCandidates() {
		return modelCandidates == null ? new String[0] : modelCandidates.clone();
	}

	public static final class Builder {
		private final SpeakerConfig c = new SpeakerConfig();

		public Builder modelCandidates(String... names) {
			c.modelCandidates = names;
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

		public Builder threshold(float t) {
			c.threshold = t;
			return this;
		}

		public Builder embeddingTimeoutMs(long ms) {
			c.embeddingTimeoutMs = ms;
			return this;
		}

		public SpeakerConfig build() {
			return c;
		}
	}
}
