package net.dreamlu.mica.voice.speaker;

import lombok.Value;

/**
 * 1:1 验证结果。
 *
 * @author dreamlu
 */
@Value
public class VerificationResult {

	/**
	 * 待验证的说话人名称。
	 */
	String speakerName;
	/**
	 * 相似度得分（0~1 之间，含义依 sherpa-onnx 而定）。
	 */
	float score;
	/**
	 * 是否通过阈值（true / false）。
	 */
	boolean matched;
	/**
	 * 判定阈值，与 {@link SpeakerConfig#getThreshold()} 一致。
	 */
	float threshold;
}
