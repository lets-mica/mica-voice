package net.dreamlu.mica.voice.tts;

import java.util.function.Consumer;

/**
 * TTS（语音合成）服务接口。
 *
 * <p>提供同步合成与回调式流式合成两种能力。
 *
 * @author dreamlu
 */
public interface TtsService extends AutoCloseable {

	/**
	 * 同步合成（一次性返回完整音频）。
	 *
	 * @param text 待合成的文本
	 * @return TTS 合成结果
	 */
	TtsAudio synthesize(String text);

	/**
	 * 同步合成，指定说话人 id（语速取默认）。
	 *
	 * @param text      待合成的文本
	 * @param speakerId 说话人 id
	 * @return TTS 合成结果
	 */
	TtsAudio synthesize(String text, int speakerId);

	/**
	 * 同步合成，指定说话人 id 与语速。
	 *
	 * @param text      待合成的文本
	 * @param speakerId 说话人 id
	 * @param speed     语速（1.0 = 默认）
	 * @return TTS 合成结果
	 */
	TtsAudio synthesize(String text, int speakerId, float speed);

	/**
	 * 回调式流式合成：边合成边回调，适合实时播放场景。
	 *
	 * @param text     待合成的文本
	 * @param callback 每次累计新增 sample 到达一定步长（由 {@code callbackSampleStep} 控制）时触发
	 * @return 完整合成结果
	 */
	TtsAudio synthesizeWithCallback(String text, java.util.function.Consumer<float[]> callback);

	/**
	 * 模型采样率（Hz）。
	 *
	 * @return 采样率
	 */
	int getSampleRate();

	/**
	 * 模型支持的说话人数量。
	 *
	 * @return 说话人数量
	 */
	int getNumSpeakers();

	@Override
	void close();
}
