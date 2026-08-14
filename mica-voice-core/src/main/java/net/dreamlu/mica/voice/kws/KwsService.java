package net.dreamlu.mica.voice.kws;

import net.dreamlu.mica.voice.audio.AudioData;

import java.util.List;

/**
 * 关键词唤醒（KWS / Keyword Spotting）服务接口。
 *
 * <p>典型用法：
 * <pre>
 *   try (KwsService kws = MicaVoice.kws(props, cfg)) {
 *       KwsResult r = kws.spot(audio);
 *       if (r != null) System.out.println("命中: " + r.getKeyword());
 *   }
 * </pre>
 *
 * @author dreamlu
 */
public interface KwsService extends AutoCloseable {

	/**
	 * 对完整音频做关键词识别，返回所有命中（按出现顺序）。
	 */
	List<KwsResult> spot(AudioData audio);

	@Override
	void close();
}
