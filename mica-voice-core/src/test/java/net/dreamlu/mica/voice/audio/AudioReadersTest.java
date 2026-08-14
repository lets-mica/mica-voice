package net.dreamlu.mica.voice.audio;

import net.dreamlu.mica.voice.exception.AudioFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AudioReaders} 单元测试。
 *
 * <p>{@link AudioReaders#read(File)} 依赖 sherpa-onnx 的 native {@code WaveReader}，
 * 这里只测无需 native 的两条路径：
 * <ul>
 *     <li>不存在的文件 → 抛 {@link AudioFormatException}</li>
 *     <li>{@link AudioReaders#fromPcm16(byte[], int)} 的边界条件</li>
 * </ul>
 */
class AudioReadersTest {

	@Test
	void read_missingFile(@TempDir Path tmp) {
		File missing = tmp.resolve("no-such.wav").toFile();
		AudioFormatException ex = assertThrows(AudioFormatException.class,
			() -> AudioReaders.read(missing));
		// 错误信息应包含文件路径
		assert ex.getMessage().contains(missing.getAbsolutePath());
	}

	@Test
	void fromPcm16_zeroBytes() {
		// 0 字节（偶数）应正常返回空 samples
		AudioData a = AudioReaders.fromPcm16(new byte[0], 16000);
		assertEquals(0, a.getSamples().length);
		assertEquals(16000, a.getSampleRate());
	}

	@Test
	void fromPcm16_oddLength() {
		// 奇数长度应抛 AudioFormatException
		assertThrows(AudioFormatException.class,
			() -> AudioReaders.fromPcm16(new byte[]{1, 2, 3}, 16000));
	}

	@Test
	void fromPcm16_endianness() {
		// 验证小端序：0x00 0x80 → -32768 → -1.0
		byte[] pcm = new byte[]{0x00, (byte) 0x80};
		AudioData a = AudioReaders.fromPcm16(pcm, 16000);
		assertEquals(1, a.getSamples().length);
		assertEquals(-1.0f, a.getSamples()[0], 1e-4);
	}

	@Test
	void read_invalidBytes(@TempDir Path tmp) throws Exception {
		// 写一个非 wav 文件（纯文本），WaveReader 应抛异常被包装成 AudioFormatException
		Path fake = tmp.resolve("fake.wav");
		Files.write(fake, "this is not a wav file".getBytes(UTF_8));
		assertThrows(AudioFormatException.class,
			() -> AudioReaders.read(fake.toFile()));
	}
}
