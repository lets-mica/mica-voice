package net.dreamlu.mica.voice.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AudioDataTest {

	@Test
	void duration() {
		AudioData a = new AudioData(new float[16000], 16000);
		assertEquals(1.0f, a.durationSeconds(), 1e-6);
	}

	@Test
	void invalidSampleRate() {
		assertThrows(IllegalArgumentException.class,
			() -> new AudioData(new float[1], 0));
	}

	@Test
	void fromPcm16() {
		byte[] pcm = new byte[]{
			0x00, 0x00,    // 0
			(byte) 0x00, (byte) 0x80,   // -32768 -> -1.0
		};
		AudioData a = AudioReaders.fromPcm16(pcm, 16000);
		assertEquals(2, a.getSamples().length);
		assertEquals(16000, a.getSampleRate());
		assertEquals(0.0f, a.getSamples()[0], 1e-6);
		assertTrue(a.getSamples()[1] <= -0.99f && a.getSamples()[1] >= -1.0f);
	}

	@Test
	void fromPcm16_invalidLength() {
		assertThrows(net.dreamlu.mica.voice.exception.AudioFormatException.class,
			() -> AudioReaders.fromPcm16(new byte[]{1, 2, 3}, 16000));
	}
}
