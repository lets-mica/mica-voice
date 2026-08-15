package net.dreamlu.mica.voice.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import net.dreamlu.mica.voice.config.SpeakerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * mica-voice Spring Boot 配置树，对应 {@code mica.voice.*}。
 *
 * <p>设计原则：
 * <ul>
 *     <li>每个能力（ASR / TTS / Speaker / VAD）都有独立 {@code enabled} 开关，默认按需启用</li>
 *     <li>嵌套配置（{@link NestedConfigurationProperty}）会随外层 enabled 一起被 Spring 解析</li>
 *     <li>运行时真正装配时，{@link net.dreamlu.mica.voice.autoconfigure.MicaVoiceAutoConfiguration}
 *         会把这里的内容转成 core 的 {@link net.dreamlu.mica.voice.config.MicaVoiceConfig}
 *         + 各能力 Config 后传给 {@link net.dreamlu.mica.voice.core.MicaVoice} 门面</li>
 * </ul>
 *
 * @author dreamlu
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mica.voice")
public class MicaVoiceProperties {

	/**
	 * 全局开关：false 时所有能力都不装配（默认 true）
	 */
	private boolean enabled = true;

	/**
	 * 模型根目录。默认 {@code ./models}
	 */
	private String modelsDir = "models";

	/**
	 * 输出目录。默认 {@code ./output}
	 */
	private String outputDir = "output";

	/**
	 * 全局线程数；各能力可单独覆盖
	 */
	private Integer threads = 2;

	/**
	 * 全局 debug 开关；各能力可单独覆盖
	 */
	private boolean debug = false;

	@NestedConfigurationProperty
	private Asr asr = new Asr();

	@NestedConfigurationProperty
	private Tts tts = new Tts();

	@NestedConfigurationProperty
	private Speaker speaker = new Speaker();

	@NestedConfigurationProperty
	private Vad vad = new Vad();

	@NestedConfigurationProperty
	private Diarization diarization = new Diarization();

	@NestedConfigurationProperty
	private Kws kws = new Kws();

	@NestedConfigurationProperty
	private Denoise denoise = new Denoise();

	/**
	 * ASR 配置。
	 */
	@Getter
	@Setter
	public static class Asr {

		@NestedConfigurationProperty
		private Offline offline = new Offline();

		@NestedConfigurationProperty
		private Online online = new Online();

		/**
		 * 离线 ASR 配置。
		 */
		@Getter
		@Setter
		public static class Offline {
			/**
			 * 离线 ASR 总开关
			 */
			private boolean enabled = true;
			/**
			 * 模型目录名（位于 {@code modelsDir/} 下）
			 */
			private String modelDirName = "sherpa-onnx-paraformer-zh-small-2024-03-09";
			/**
			 * 模型家族（PARAFORMER / SENSE_VOICE / WHISPER / MOONSHINE / ZIPFORMER / NEMO_CTC / AUTO）
			 */
			private String modelType = "PARAFORMER";
			/**
			 * 线程数；为空则用全局 threads
			 */
			private Integer threads;
			private boolean debug;
			/**
			 * SenseVoice / Whisper 专用：auto/zh/en/ja/ko/yue
			 */
			private String language = "auto";
			/**
			 * SenseVoice 专用：是否做逆文本规范化
			 */
			private boolean inverseTextNormalization = true;
		}

		/**
		 * 在线流式 ASR 配置。
		 */
		@Getter
		@Setter
		public static class Online {
			/**
			 * 在线流式 ASR 总开关
			 */
			private boolean enabled = false;
			private String modelDirName = "sherpa-onnx-streaming-paraformer-bilingual-zh-en";
			/**
			 * 模型家族：
			 * <ul>
			 * <li>PARAFORMER（默认，流式 Paraformer）</li>
			 * <li>X_ASR（上海交大 Zipformer Transducer，960ms chunk）</li>
			 * <li>ZIPFORMER / ZIPFORMER2_CTC / NEMO_CTC / TRANSDUCER</li>
			 * <li>AUTO（自动根据目录文件推断）</li>
			 * </ul>
			 */
			private String modelType = "PARAFORMER";
			private Integer threads;
			private boolean debug;
			private boolean enableEndpoint = true;
			/**
			 * 端点规则 1：最短尾部静音（秒）。小于该静音段即判定为 endpoint。
			 * 默认 2.4s（sherpa-onnx 默认），调小可更快识别出"说完了"。
			 */
			private Double endpointRule1MinTrailingSilence;
			/**
			 * 端点规则 2：最短尾部静音（秒）。默认 1.2s。
			 */
			private Double endpointRule2MinTrailingSilence;
			/**
			 * 端点规则 3：最短语音长度（秒），过短视为噪音不触发。默认 0.2s。
			 */
			private Double endpointRule3MinUtteranceLength;
			/**
			 * 流式分块大小（采样数）；默认 1600 ≈ 100ms @ 16kHz
			 */
			private int chunkSize = 1600;
		}
	}

	/**
	 * TTS 配置。
	 */
	@Getter
	@Setter
	public static class Tts {
		private boolean enabled = true;
		private String modelDirName = "vits-icefall-zh-aishell3";
		private String modelType = "VITS";
		private Integer threads;
		private boolean debug;
		private int defaultSpeakerId = 0;
		private float defaultSpeed = 1.0f;
		private int callbackSampleStep = 1600;
	}

	/**
	 * 声纹识别配置。
	 */
	@Getter
	@Setter
	public static class Speaker {
		private boolean enabled = true;
		/**
		 * 阈值（cosine），超过判定同一人
		 */
		private float threshold = 0.5f;
		private Integer threads;
		private boolean debug;
		/**
		 * 候选模型名（按优先级），用 {@code modelsDir/} 根目录或同名子目录
		 */
		private String[] modelCandidates = SpeakerConfig.DEFAULT_MODEL_CANDIDATES;
		private long embeddingTimeoutMs = 30_000L;
	}

	/**
	 * 语音端点检测（VAD）配置。
	 */
	@Getter
	@Setter
	public static class Vad {
		/**
		 * 扩展能力
		 */
		private boolean enabled = false;
		private String modelFileName = "silero_vad.onnx";
		private String modelType = "SILERO";
		private int sampleRate = 16000;
		private Integer threads;
		private boolean debug;
		private float threshold = 0.5f;
		private float minSilenceDuration = 0.5f;
		private float minSpeechDuration = 0.25f;
		private float maxSpeechDuration = 20.0f;
		private int windowSize = 512;
	}

	/**
	 * 说话人分离配置。
	 */
	@Getter
	@Setter
	public static class Diarization {
		/**
		 * 扩展能力
		 */
		private boolean enabled = false;
		private String segmentationModelFileName = "sherpa-onnx-pyannote-segmentation-3-0.onnx";
		private String embeddingModelFileName = "3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx";
		private Integer threads;
		private boolean debug;
		private int numClusters = 0;
		private float clusterThreshold = 0.5f;
		private float minDurationOff = 0.5f;
		private float minDurationOn = 0.3f;
	}

	/**
	 * 关键词唤醒（KWS）配置。
	 */
	@Getter
	@Setter
	public static class Kws {
		/**
		 * 扩展能力
		 */
		private boolean enabled = false;
		private String modelDirName = "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23";
		private Integer threads;
		private boolean debug;
		private int sampleRate = 16000;
		private int featureDim = 80;
		private float keywordsScore = 2.0f;
		private float keywordsThreshold = 0.25f;
		private int maxActivePaths = 4;
		private String keywordsFile = "keywords.txt";
	}

	/**
	 * 音频降噪配置。
	 */
	@Getter
	@Setter
	public static class Denoise {
		/**
		 * 扩展能力
		 */
		private boolean enabled = false;
		private String modelFileName = "sherpa-onnx-gtcrn.onnx";
		private String modelType = "GTCRN";
		private Integer threads;
		private boolean debug;
		private float attenuationLimitDb = 12.0f;
	}
}
