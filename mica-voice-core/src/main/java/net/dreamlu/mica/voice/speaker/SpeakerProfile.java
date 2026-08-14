package net.dreamlu.mica.voice.speaker;

import lombok.Getter;
import lombok.NonNull;

/**
 * 注册成功的说话人（不可变）。
 *
 * @author dreamlu
 */
@Getter
public final class SpeakerProfile {

	@NonNull
	private final String name;
	@NonNull
	private final float[] embedding;

	public SpeakerProfile(String name, float[] embedding) {
		this.name = name;
		this.embedding = embedding.clone();
	}

	public String getName() {
		return name;
	}

	/**
	 * 防御性拷贝，避免外部修改内部状态。
	 */
	public float[] getEmbedding() {
		return embedding.clone();
	}

	@Override
	public String toString() {
		return "SpeakerProfile{name='" + name + "', dim=" + embedding.length + "}";
	}
}
