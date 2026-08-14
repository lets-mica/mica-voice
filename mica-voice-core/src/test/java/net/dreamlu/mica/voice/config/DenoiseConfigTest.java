package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DenoiseConfigTest {

	@Test
	void defaults() {
		DenoiseConfig c = new DenoiseConfig();
		assertEquals(DenoiseConfig.ModelType.GTCRN, c.getModelType());
		assertEquals(12.0f, c.getAttenuationLimitDb());
	}

	@Test
	void builder() {
		DenoiseConfig c = DenoiseConfig.builder()
			.modelType(DenoiseConfig.ModelType.DPDFNet)
			.attenuationLimitDb(20.0f)
			.build();
		assertEquals(DenoiseConfig.ModelType.DPDFNet, c.getModelType());
		assertEquals(20.0f, c.getAttenuationLimitDb());
	}
}
