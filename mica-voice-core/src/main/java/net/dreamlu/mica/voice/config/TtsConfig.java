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

	private String modelDirName;

	public TtsConfig(String modelDirName) {
		this.modelDirName = modelDirName;
	}

	@Builder.Default
	private ModelType modelType = ModelType.VITS;
	private Integer threads;
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

	public enum ModelType {
		VITS,
		MATCHA,
		KOKORO,
		/**
		 * 自动（v1 仅支持 VITS，留作扩展）
		 */
		AUTO
	}

	/**
	 * null → VITS 的兜底（setter 路径）。
	 */
	public void setModelType(ModelType modelType) {
		this.modelType = modelType == null ? ModelType.VITS : modelType;
	}
}