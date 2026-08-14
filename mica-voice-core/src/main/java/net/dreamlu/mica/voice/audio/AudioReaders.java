package net.dreamlu.mica.voice.audio;

import com.k2fsa.sherpa.onnx.WaveReader;
import net.dreamlu.mica.voice.exception.AudioFormatException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 音频读取工具。
 *
 * <p>Phase 1：直接复用 sherpa-onnx 的 {@code WaveReader}，并支持从 {@code File / Path / byte[]}
 * 三种来源加载 WAV。对非 wav（mp3/m4a/flac 等）抛 {@link AudioFormatException}，
 * 提示用户先用 ffmpeg 转换。
 *
 * @author dreamlu
 */
public final class AudioReaders {

	private AudioReaders() {
	}

	/**
	 * 从文件读取 wav。
	 */
	public static AudioData read(File file) {
		if (!file.isFile()) {
			throw new AudioFormatException("音频文件不存在: " + file.getAbsolutePath());
		}
		return read(file.getAbsolutePath());
	}

	/**
	 * 从文件路径读取 wav。
	 */
	public static AudioData read(String path) {
		try {
			WaveReader reader = new WaveReader(path);
			return new AudioData(reader.getSamples(), reader.getSampleRate());
		} catch (Throwable t) {
			// sherpa-onnx 的 WaveReader 对 mp3/非 wav 会抛 IllegalArgumentException
			throw new AudioFormatException(
				"无法读取音频: " + path + "（sherpa-onnx 仅支持单声道 16-bit WAV，其它格式请先用 ffmpeg 转换）", t);
		}
	}

	/**
	 * 从内存中的完整 wav 字节流读取（适合 Web 上传后直接识别）。
	 * <p>Phase 1 简单实现：先写到临时文件再调 WaveReader。
	 */
	public static AudioData read(byte[] wavBytes) {
		Path tmp;
		try {
			tmp = Files.createTempFile("mica-voice-", ".wav");
			Files.write(tmp, wavBytes);
			File f = tmp.toFile();
			f.deleteOnExit();
			return read(f);
		} catch (IOException e) {
			throw new AudioFormatException("无法从内存读取 wav: " + e.getMessage(), e);
		}
	}

	/**
	 * 直接从原始 PCM（16-bit signed LE, 单声道）构造 AudioData。
	 * 适合已经预处理过的音频流。
	 */
	public static AudioData fromPcm16(byte[] pcm16, int sampleRate) {
		if (pcm16.length % 2 != 0) {
			throw new AudioFormatException("PCM16 数据长度必须是 2 的倍数");
		}
		ByteBuffer buf = ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN);
		int n = pcm16.length / 2;
		float[] samples = new float[n];
		for (int i = 0; i < n; i++) {
			samples[i] = buf.getShort() / 32768.0f;
		}
		return new AudioData(samples, sampleRate);
	}
}
