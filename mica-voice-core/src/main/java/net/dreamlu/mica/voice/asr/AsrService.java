package net.dreamlu.mica.voice.asr;

import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.exception.AudioFormatException;

import java.io.File;

/**
 * 语音识别（ASR）服务接口。
 *
 * <p>对外暴露最常用的同步识别能力，流式识别请用 {@link OnlineAsrService}。
 *
 * @author dreamlu
 */
public interface AsrService extends AutoCloseable {

	/**
	 * 从文件识别（同步）。
	 *
	 * @param wav wav 文件
	 * @return 识别结果
	 */
	default AsrResult recognize(File wav) {
		return recognize(wav.getAbsolutePath());
	}

	/**
	 * 从 wav 文件路径识别（同步）。
	 *
	 * @param wavPath wav 文件路径
	 * @return 识别结果
	 */
	AsrResult recognize(String wavPath);

	/**
	 * 从完整的 wav 字节流识别（适合 Web 上传后直接识别）。
	 *
	 * @param wavBytes 完整的 wav 字节流
	 * @return 识别结果
	 */
	default AsrResult recognize(byte[] wavBytes) {
		try {
			return recognize(AudioReaders.read(wavBytes));
		} catch (AudioFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new AudioFormatException("无法读取上传的 wav 字节流", e);
		}
	}

	/**
	 * 从内存 AudioData 识别（同步）。适合 Web 上传后预处理过的音频流。
	 *
	 * @param audio 音频数据
	 * @return 识别结果
	 */
	AsrResult recognize(AudioData audio);

	/**
	 * 关闭并释放 native 资源。重复调用安全。
	 */
	@Override
	void close();
}
