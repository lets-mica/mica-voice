package net.dreamlu.mica.voice.kws;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 一次关键词识别结果（不可变）。
 *
 * @author dreamlu
 */
@Value
@Builder
public class KwsResult {

	/**
	 * 命中的关键词文本。
	 */
	String keyword;
	/**
	 * 命中的 token 序列（用于调试）。
	 */
	List<String> tokens;
	/**
	 * 时间戳（秒），与 {@link #tokens} 一一对应。
	 */
	float[] timestamps;
	/**
	 * 触发时的累计样本偏移（用于接 WebSocket 流式场景）。
	 */
	long triggeredAtSample;
}
