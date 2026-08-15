package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

/**
 * 在线流式 ASR 配置。
 *
 * <p>默认支持 Streaming Paraformer；扩展支持 X-ASR（Zipformer Transducer，
 * 上海交大开源的 160M 参数中英流式模型）等多种模型族。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class OnlineAsrConfig {

	/**
	 * 模型目录名（位于全局 modelsDir 下的子目录）。
	 */
	private String modelDirName;
	/**
	 * 流式模型家族。
	 */
	private ModelType modelType = ModelType.PARAFORMER;
	/**
	 * 推理线程数；为 null 时回退到 {@link MicaVoiceConfig#getThreads()}。
	 */
	private Integer threads;
	/**
	 * 是否输出 sherpa-onnx 调试日志。
	 */
	private boolean debug;
	/**
	 * 是否启用 endpoint（句尾静音自动结束）
	 */
	private boolean enableEndpoint = true;
	/**
	 * 端点规则 1 最短尾部静音（秒）。null 表示使用 sherpa-onnx 默认。
	 */
	private Double endpointRule1MinTrailingSilence;
	/**
	 * 端点规则 2 最短尾部静音（秒）。null 表示使用 sherpa-onnx 默认。
	 */
	private Double endpointRule2MinTrailingSilence;
	/**
	 * 端点规则 3 最短语音长度（秒），过短视为噪音不触发。null 表示使用默认。
	 */
	private Double endpointRule3MinUtteranceLength;
	/**
	 * 流式输入分块大小（采样点数）；默认 1600（= 16kHz * 100ms）
	 */
	private int chunkSize = 1600;
	/**
	 * 特征采样率（Hz）
	 */
	private int sampleRate = 16000;
	/**
	 * 特征维度
	 */
	private int featureDim = 80;
	/**
	 * 解码方法（greedy_search / modified_beam_search）
	 */
	private String decodingMethod = "greedy_search";
	/**
	 * encoder 文件名候选（按优先级）。X-ASR 形如 {@code encoder-960ms.onnx}，
	 * 普通流式 Paraformer 形如 {@code encoder.onnx} / {@code encoder.int8.onnx}。
	 */
	private String[] encoderCandidates = new String[]{
		"encoder-960ms.onnx",
		"encoder.int8.onnx",
		"encoder.onnx"
	};
	/**
	 * decoder 文件名候选
	 */
	private String[] decoderCandidates = new String[]{
		"decoder-960ms.onnx",
		"decoder.int8.onnx",
		"decoder.onnx"
	};
	/**
	 * joiner 文件名候选（X-ASR 与通用 Transducer 必填）
	 */
	private String[] joinerCandidates = new String[]{
		"joiner-960ms.onnx",
		"joiner.int8.onnx",
		"joiner.onnx"
	};
	public OnlineAsrConfig() {
	}

	/**
	 * 便捷构造：仅指定模型目录。
	 *
	 * @param modelDirName 模型目录名
	 */
	public OnlineAsrConfig(String modelDirName) {
		this.modelDirName = modelDirName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType == null ? ModelType.AUTO : modelType;
	}

	public void setEncoderCandidates(String[] encoderCandidates) {
		this.encoderCandidates = encoderCandidates == null ? new String[0] : encoderCandidates.clone();
	}

	public void setDecoderCandidates(String[] decoderCandidates) {
		this.decoderCandidates = decoderCandidates == null ? new String[0] : decoderCandidates.clone();
	}

	public void setJoinerCandidates(String[] joinerCandidates) {
		this.joinerCandidates = joinerCandidates == null ? new String[0] : joinerCandidates.clone();
	}

	/**
	 * 把 ModelType 映射成 sherpa-onnx 的 modelType 字符串。
	 * 例如 X_ASR / ZIPFORMER2_CTC → "zipformer2"，ZIPFORMER → "zipformer"，PARAFORMER → null（走默认）。
	 *
	 * @return sherpa-onnx 的 modelType 字符串；没有对应值返回 null
	 */
	public String toSherpaModelType() {
		if (modelType == null) return null;
		switch (modelType) {
			case X_ASR:
			case ZIPFORMER2_CTC:
				return "zipformer2";
			case ZIPFORMER:
				return "zipformer";
			default:
				return null;
		}
	}

	public enum ModelType {
		/**
		 * 流式 Paraformer（encoder + decoder 拆分）
		 */
		PARAFORMER,
		/**
		 * X-ASR（Zipformer Transducer，960ms chunk，encoder + decoder + joiner 三段式）
		 */
		X_ASR,
		/**
		 * 通用 Zipformer CTC
		 */
		ZIPFORMER,
		/**
		 * Zipformer2 CTC
		 */
		ZIPFORMER2_CTC,
		/**
		 * NeMo CTC
		 */
		NEMO_CTC,
		/**
		 * 通用 Transducer（encoder + decoder + joiner 三段式）
		 */
		TRANSDUCER,
		/**
		 * 自动根据模型文件推断
		 */
		AUTO
	}

	public static final class Builder {
		private final OnlineAsrConfig c = new OnlineAsrConfig();

		public Builder modelDirName(String name) {
			c.modelDirName = name;
			return this;
		}

		public Builder modelType(ModelType t) {
			c.modelType = t;
			return this;
		}

		public Builder modelType(String raw) {
			if (raw == null || raw.isEmpty()) {
				c.modelType = ModelType.AUTO;
				return this;
			}
			try {
				c.modelType = ModelType.valueOf(raw.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ex) {
				c.modelType = ModelType.AUTO;
			}
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

		public Builder enableEndpoint(boolean b) {
			c.enableEndpoint = b;
			return this;
		}

		public Builder endpointRule1MinTrailingSilence(Double v) {
			c.endpointRule1MinTrailingSilence = v;
			return this;
		}

		public Builder endpointRule2MinTrailingSilence(Double v) {
			c.endpointRule2MinTrailingSilence = v;
			return this;
		}

		public Builder endpointRule3MinUtteranceLength(Double v) {
			c.endpointRule3MinUtteranceLength = v;
			return this;
		}

		public Builder chunkSize(int n) {
			c.chunkSize = n;
			return this;
		}

		public Builder sampleRate(int n) {
			c.sampleRate = n;
			return this;
		}

		public Builder featureDim(int n) {
			c.featureDim = n;
			return this;
		}

		public Builder decodingMethod(String s) {
			c.decodingMethod = s;
			return this;
		}

		public Builder encoderCandidates(String... names) {
			c.encoderCandidates = names;
			return this;
		}

		public Builder decoderCandidates(String... names) {
			c.decoderCandidates = names;
			return this;
		}

		public Builder joinerCandidates(String... names) {
			c.joinerCandidates = names;
			return this;
		}

		public OnlineAsrConfig build() {
			return c;
		}
	}
}
