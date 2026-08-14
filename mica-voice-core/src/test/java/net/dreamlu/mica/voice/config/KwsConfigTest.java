package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KwsConfigTest {

	@Test
	void defaults() {
		KwsConfig c = new KwsConfig();
		assertEquals(16000, c.getSampleRate());
		assertEquals(80, c.getFeatureDim());
		assertEquals(2.0f, c.getKeywordsScore());
		assertEquals(0.25f, c.getKeywordsThreshold());
	}

	@Test
	void builder() {
		KwsConfig c = KwsConfig.builder()
			.sampleRate(8000)
			.featureDim(40)
			.keywordsThreshold(0.5f)
			.build();
		assertEquals(8000, c.getSampleRate());
		assertEquals(40, c.getFeatureDim());
		assertEquals(0.5f, c.getKeywordsThreshold());
	}
}
