package net.dreamlu.mica.voice.speaker;

import lombok.Value;

/**
 * 1:N 搜索结果。
 *
 * <p>如果没有任何说话人匹配（分数均低于阈值），{@link #speakerName} 为 null。
 *
 * @author dreamlu
 */
@Value
public class SearchResult {

	/**
	 * 命中的说话人名称；无匹配时为 null。
	 */
	String speakerName;
	/**
	 * 相似度得分（0~1 之间，含义依 sherpa-onnx 而定）。
	 */
	float score;
	/**
	 * 判定阈值，与 {@link SpeakerConfig#getThreshold()} 一致。
	 */
	float threshold;

	/**
	 * 构造一个空命中结果（无匹配）。
	 *
	 * @param threshold 判定阈值
	 * @return speakerName 为 null 的 SearchResult
	 */
	public static SearchResult empty(float threshold) {
		return new SearchResult(null, 0f, threshold);
	}

	/**
	 * 是否命中了已注册的说话人。
	 *
	 * @return 命中返回 true，否则返回 false
	 */
	public boolean isMatched() {
		return speakerName != null;
	}
}
