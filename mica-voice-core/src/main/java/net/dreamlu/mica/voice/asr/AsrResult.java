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

	String text;
	@Builder.Default
	List<String> tokens = Collections.emptyList();
	String language;
	String emotion;
	String event;
	long costMs;
}
