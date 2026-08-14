package net.dreamlu.mica.voice.tts;

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
	 */
	TtsAudio synthesize(String text);

	TtsAudio synthesize(String text, int speakerId);

	TtsAudio synthesize(String text, int speakerId, float speed);

	/**
	 * 回调式流式合成：边合成边回调，适合实时播放场景。
	 *
	 * @param callback 每次累计新增 sample 到达一定步长（由 {@code callbackSampleStep} 控制）时触发
	 */
	TtsAudio synthesizeWithCallback(String text, java.util.function.Consumer<float[]> callback);

	/**
	 * 模型采样率（Hz）。
	 */
	int getSampleRate();

	/**
	 * 模型支持的说话人数量。
	 */
	int getNumSpeakers();

	@Override
	void close();
}
