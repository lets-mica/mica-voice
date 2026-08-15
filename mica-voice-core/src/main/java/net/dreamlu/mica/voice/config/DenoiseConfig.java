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

	@Builder.Default
	private String modelFileName = "sherpa-onnx-gtcrn.onnx";
	@Builder.Default
	private ModelType modelType = ModelType.GTCRN;
	private Integer threads;
	private boolean debug;
	/**
	 * 仅 DPDFNet：衰减限制（dB），控制降噪强度。
	 */
	@Builder.Default
	private float attenuationLimitDb = 12.0f;

	public enum ModelType {
		GTCRN,
		DPDFNet
	}
}