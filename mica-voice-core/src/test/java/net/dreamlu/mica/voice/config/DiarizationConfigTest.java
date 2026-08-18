package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiarizationConfigTest {

	@Test
	void defaults() {
		DiarizationConfig c = new DiarizationConfig();
		assertEquals(0, c.getNumClusters());
		assertEquals(0.5f, c.getClusterThreshold());
		assertEquals(0.1f, c.getWindowShiftRatio());
	}

	@Test
	void builder() {
		DiarizationConfig c = DiarizationConfig.builder()
			.numClusters(4)
			.clusterThreshold(0.7f)
			.minDurationOff(0.4f)
			.windowShiftRatio(0.25f)
			.build();
		assertEquals(4, c.getNumClusters());
		assertEquals(0.7f, c.getClusterThreshold());
		assertEquals(0.4f, c.getMinDurationOff());
		assertEquals(0.25f, c.getWindowShiftRatio());
	}
}
