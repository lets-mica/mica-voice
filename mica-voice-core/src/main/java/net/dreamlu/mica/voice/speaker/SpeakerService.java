package net.dreamlu.mica.voice.speaker;

import net.dreamlu.mica.voice.audio.AudioData;

import java.io.File;
import java.util.List;

/**
 * 声纹识别服务接口。
 *
 * <p>核心能力：
 * <ul>
 *     <li>{@link #enroll(String, File)} / {@link #enroll(String, AudioData)}：注册说话人</li>
 *     <li>{@link #verify(String, File)}：1:1 验证</li>
 *     <li>{@link #search(File)}：1:N 搜索（返回最相似的说话人）</li>
 *     <li>{@link #remove(String)} / {@link #names()}：管理已注册的说话人</li>
 * </ul>
 *
 * @author dreamlu
 */
public interface SpeakerService extends AutoCloseable {

	/**
	 * 注册：说话人名字 + 音频，返回该说话人的嵌入快照。
	 */
	SpeakerProfile enroll(String name, File wav);

	SpeakerProfile enroll(String name, AudioData audio);

	/**
	 * 1:1 验证：测试音频是否属于指定说话人。
	 */
	VerificationResult verify(String name, File wav);

	VerificationResult verify(String name, AudioData audio);

	/**
	 * 1:N 搜索：测试音频最像已注册中的谁（可能无匹配）。
	 */
	SearchResult search(File wav);

	SearchResult search(AudioData audio);

	/**
	 * 已注册的说话人列表。
	 */
	List<String> names();

	/**
	 * 当前已注册人数。
	 */
	int size();

	/**
	 * 移除说话人。
	 */
	boolean remove(String name);

	@Override
	void close();
}
