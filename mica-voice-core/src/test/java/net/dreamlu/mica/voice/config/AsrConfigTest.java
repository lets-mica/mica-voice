package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsrConfigTest {

	@Test
	void defaults() {
		AsrConfig c = new AsrConfig();
		assertEquals(AsrConfig.ModelType.PARAFORMER, c.getModelType());
		assertEquals("auto", c.getLanguage());
		assertTrue(c.isInverseTextNormalization());
	}

	@Test
	void builder() {
		AsrConfig c = AsrConfig.builder()
			.modelDirName("a")
			.modelType(AsrConfig.ModelType.SENSE_VOICE)
			.language("zh")
			.inverseTextNormalization(false)
			.threads(4)
			.build();
		assertEquals("a", c.getModelDirName());
		assertEquals(AsrConfig.ModelType.SENSE_VOICE, c.getModelType());
		assertEquals("zh", c.getLanguage());
		assertEquals(4, c.getThreads());
	}

	@Test
	void nullModelTypeFallBack() {
		AsrConfig c = new AsrConfig();
		c.setModelType(null);
		assertEquals(AsrConfig.ModelType.AUTO, c.getModelType());
	}
}
