package net.dreamlu.mica.voice.denoise;

import net.dreamlu.mica.voice.audio.AudioData;

/**
 * 音频降噪服务接口。
 *
 * @author dreamlu
 */
public interface DenoiseService extends AutoCloseable {

	/**
	 * 离线降噪：输入原始音频，返回降噪后的音频。
	 */
	DenoisedAudio denoise(AudioData audio);

	@Override
	void close();
}
