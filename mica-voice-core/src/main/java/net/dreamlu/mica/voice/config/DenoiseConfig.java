package net.dreamlu.mica.voice.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenoiseConfig {

	/**
	 * 降噪模型文件名。
	 */
	@Builder.Default
	private String modelFileName = "sherpa-onnx-gtcrn.onnx";
	/**
	 * 降噪模型家族。
	 */
	@Builder.Default
	private ModelType modelType = ModelType.GTCRN;
	/**
	 * 推理线程数；为 null 时回退到 {@link MicaVoiceConfig#getThreads()}。
	 */
	private Integer threads;
	/**
	 * 是否输出 sherpa-onnx 调试日志。
	 */
	private boolean debug;
	/**
	 * 仅 DPDFNet：衰减限制（dB），控制降噪强度。
	 */
	@Builder.Default
	private float attenuationLimitDb = 12.0f;

	/**
	 * 降噪模型家族。
	 */
	public enum ModelType {
		/**
		 * GTCRN：轻量级流式降噪
		 */
		GTCRN,
		/**
		 * DeepFilterNet（dfpdfnet）：高质量离线降噪
		 */
		DPDFNet
	}
}