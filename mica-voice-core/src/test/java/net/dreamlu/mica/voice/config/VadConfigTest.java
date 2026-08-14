package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VadConfigTest {

	@Test
	void defaults() {
		VadConfig c = new VadConfig();
		assertEquals("silero_vad.onnx", c.getModelFileName());
		assertEquals(VadConfig.ModelType.SILERO, c.getModelType());
		assertEquals(16000, c.getSampleRate());
	}

	@Test
	void builder() {
		VadConfig c = VadConfig.builder()
			.modelFileName("ten_vad.onnx")
			.modelType(VadConfig.ModelType.TEN)
			.sampleRate(8000)
			.threshold(0.6f)
			.minSilenceDuration(0.3f)
			.build();
		assertEquals("ten_vad.onnx", c.getModelFileName());
		assertEquals(VadConfig.ModelType.TEN, c.getModelType());
		assertEquals(8000, c.getSampleRate());
		assertEquals(0.6f, c.getThreshold());
	}

	@Test
	void defaultCandidatesExists() {
		assertNotNull(VadConfig.DEFAULT_MODEL_CANDIDATES);
		assertEquals("silero_vad.onnx", VadConfig.DEFAULT_MODEL_CANDIDATES[0]);
	}
}
