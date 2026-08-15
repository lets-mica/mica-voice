package net.dreamlu.mica.voice.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsConfig {

	/**
	 * 模型目录名（位于全局 modelsDir 下的子目录）。
	 */
	private String modelDirName;

	/**
	 * 便捷构造：仅指定模型目录。
	 *
	 * @param modelDirName 模型目录名
	 */
	public TtsConfig(String modelDirName) {
		this.modelDirName = modelDirName;
	}

	/**
	 * TTS 模型家族，默认 {@link ModelType#VITS}。
	 */
	@Builder.Default
	private ModelType modelType = ModelType.VITS;
	/**
	 * 推理线程数；为 null 时回退到 {@link MicaVoiceConfig#getThreads()}。
	 */
	private Integer threads;
	/**
	 * 是否输出 sherpa-onnx 调试日志。
	 */
	private boolean debug;
	/**
	 * 默认说话人 id
	 */
	@Builder.Default
	private int defaultSpeakerId = 0;
	/**
	 * 默认语速
	 */
	@Builder.Default
	private float defaultSpeed = 1.0f;
	/**
	 * 回调式合成时，每多少采样回调一次（默认 1600 ≈ 100ms @ 16kHz）
	 */
	@Builder.Default
	private int callbackSampleStep = 1600;

	/**
	 * TTS 模型家族。
	 */
	public enum ModelType {
		/**
		 * VITS（主流轻量模型）
		 */
		VITS,
		/**
		 * Matcha（Matcha-TTS，ACE 前端）
		 */
		MATCHA,
		/**
		 * Kokoro
		 */
		KOKORO,
		/**
		 * 自动（v1 仅支持 VITS，留作扩展）
		 */
		AUTO
	}

	/**
	 * null → VITS 的兜底（setter 路径）。
	 *
	 * @param modelType 模型家族，null 时回退到 {@link ModelType#VITS}
	 */
	public void setModelType(ModelType modelType) {
		this.modelType = modelType == null ? ModelType.VITS : modelType;
	}
}