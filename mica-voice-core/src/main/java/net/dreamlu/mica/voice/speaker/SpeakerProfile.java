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

	/**
	 * 说话人名称。
	 */
	private final String name;
	/**
	 * 嵌入向量（声纹特征），单位为 float 数组。
	 */
	private final float[] embedding;

	/**
	 * 构造一个说话人档案。
	 *
	 * @param name      说话人名称
	 * @param embedding 嵌入向量（内部会做防御性拷贝）
	 */
	public SpeakerProfile(@NonNull String name, float[] embedding) {
		this.name = name;
		this.embedding = embedding.clone();
	}

	public String getName() {
		return name;
	}

	/**
	 * 防御性拷贝，避免外部修改内部状态。
	 *
	 * @return 嵌入向量的拷贝
	 */
	public float[] getEmbedding() {
		return embedding.clone();
	}

	@Override
	public String toString() {
		return "SpeakerProfile{name='" + name + "', dim=" + embedding.length + "}";
	}
}
