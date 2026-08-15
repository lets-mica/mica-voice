package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 关键词唤醒（KWS / Keyword Spotting）配置。
 *
 * <p>需要 2 类文件：
 * <ul>
 *     <li>模型目录名（如 {@code sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23}）</li>
 *     <li>关键词文件（{@code keywords.txt}，每行一个关键词 + tokens）</li>
 * </ul>
 *
 * @author dreamlu
 */
@Getter
@Setter
public class KwsConfig {

	private String modelDirName = "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23";
	private Integer threads;
	private boolean debug;
	/**
	 * 特征采样率；推荐 16000
	 */
	private int sampleRate = 16000;
	/**
	 * 特征维度
	 */
	private int featureDim = 80;
	/**
	 * 关键词分数阈值
	 */
	private float keywordsScore = 2.0f;
	/**
	 * 关键词触发阈值
	 */
	private float keywordsThreshold = 0.25f;
	/**
	 * 最大激活路径数
	 */
	private int maxActivePaths = 4;
	/**
	 * 关键词文件名（位于 modelDirName 内）
	 */
	private String keywordsFile = "keywords.txt";

	public KwsConfig() {
	}

	/**
	 * 创建一个流式 Builder。
	 *
	 * @return 配置 Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final KwsConfig c = new KwsConfig();

		public Builder modelDirName(String n) {
			c.modelDirName = n;
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

		public Builder sampleRate(int v) {
			c.sampleRate = v;
			return this;
		}

		public Builder featureDim(int v) {
			c.featureDim = v;
			return this;
		}

		public Builder keywordsScore(float v) {
			c.keywordsScore = v;
			return this;
		}

		public Builder keywordsThreshold(float v) {
			c.keywordsThreshold = v;
			return this;
		}

		public Builder maxActivePaths(int v) {
			c.maxActivePaths = v;
			return this;
		}

		public Builder keywordsFile(String n) {
			c.keywordsFile = n;
			return this;
		}

		public KwsConfig build() {
			return c;
		}
	}
}
