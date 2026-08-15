package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 说话人分离（Speaker Diarization）配置。
 *
 * <p>模型结构：segmentation + embedding + clustering 三件套。
 * 推荐用 sherpa-onnx 官方 release 的 pyannote 分割模型 + 3D-Speaker embedding + 默认 fast clustering。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class DiarizationConfig {

	/**
	 * segmentation 模型候选名（Pyannote 系）。
	 */
	public static final String[] DEFAULT_SEGMENTATION_CANDIDATES = new String[]{
		"sherpa-onnx-pyannote-segmentation-3-0.onnx",
		"pyannote-3.0.onnx"
	};

	private String segmentationModelFileName = "sherpa-onnx-pyannote-segmentation-3-0.onnx";
	private String embeddingModelFileName = "3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx";
	private Integer threads;
	private boolean debug;
	/**
	 * 期望说话人数（0 = 由 clustering 自动推断）
	 */
	private int numClusters = 0;
	/**
	 * fast clustering 阈值（默认 0.5）
	 */
	private float clusterThreshold = 0.5f;
	/**
	 * 最小关闭时长（秒）：相邻两段合并的最大间隔
	 */
	private float minDurationOff = 0.5f;
	/**
	 * 最小开启时长（秒）：短于此视为噪声丢弃
	 */
	private float minDurationOn = 0.3f;

	public DiarizationConfig() {
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
		private final DiarizationConfig c = new DiarizationConfig();

		public Builder segmentationModelFileName(String n) {
			c.segmentationModelFileName = n;
			return this;
		}

		public Builder embeddingModelFileName(String n) {
			c.embeddingModelFileName = n;
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

		public Builder numClusters(int n) {
			c.numClusters = n;
			return this;
		}

		public Builder clusterThreshold(float v) {
			c.clusterThreshold = v;
			return this;
		}

		public Builder minDurationOff(float v) {
			c.minDurationOff = v;
			return this;
		}

		public Builder minDurationOn(float v) {
			c.minDurationOn = v;
			return this;
		}

		public DiarizationConfig build() {
			return c;
		}
	}
}
