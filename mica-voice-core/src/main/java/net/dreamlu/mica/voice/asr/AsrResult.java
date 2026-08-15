package net.dreamlu.mica.voice.asr;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

/**
 * 离线 ASR 识别结果（不可变）。
 *
 * <p>包含主文本，以及 sherpa-onnx 部分模型（如 SenseVoice）会输出的
 * 语言 / 情感 / 事件 等元信息。
 *
 * @author dreamlu
 */
@Value
@Builder
public class AsrResult {

	/**
	 * 识别出的完整文本。
	 */
	String text;
	/**
	 * 识别出的 token 序列（用于调试或自定义前端处理）。
	 */
	@Builder.Default
	List<String> tokens = Collections.emptyList();
	/**
	 * 模型识别的语种（如 SenseVoice 输出 auto / zh / en 等），其他模型可能为 null。
	 */
	String language;
	/**
	 * 模型识别的情感（如 SenseVoice 输出），其他模型可能为 null。
	 */
	String emotion;
	/**
	 * 模型识别的事件标签（如 SenseVoice 输出 Music / Speech / Cry 等），其他模型可能为 null。
	 */
	String event;
	/**
	 * 本次识别耗时（毫秒）。
	 */
	long costMs;
}
