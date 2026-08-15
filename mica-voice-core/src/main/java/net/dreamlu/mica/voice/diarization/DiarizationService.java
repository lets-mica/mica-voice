package net.dreamlu.mica.voice.diarization;

import net.dreamlu.mica.voice.audio.AudioData;

import java.util.List;

/**
 * 说话人分离服务接口。
 *
 * <p>输入一整段音频，输出"哪个时间区间是谁说的"列表。
 *
 * @author dreamlu
 */
public interface DiarizationService extends AutoCloseable {

	/**
	 * 对一段完整音频做说话人分离。
	 *
	 * @param audio 待分析的完整音频
	 * @return 说话人片段列表（按时间顺序）
	 */
	List<DiarizationSegment> diarize(AudioData audio);

	@Override
	void close();
}
