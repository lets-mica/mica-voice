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

	String speakerName;
	float score;
	float threshold;

	public static SearchResult empty(float threshold) {
		return new SearchResult(null, 0f, threshold);
	}

	public boolean isMatched() {
		return speakerName != null;
	}
}
