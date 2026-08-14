package net.dreamlu.mica.voice.speaker;

import lombok.Value;

/**
 * 1:1 验证结果。
 *
 * @author dreamlu
 */
@Value
public class VerificationResult {

	String speakerName;
	float score;
	boolean matched;
	float threshold;
}
