package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class MicaVoicePropertiesTest {

	@Test
	void defaults() {
		MicaVoiceProperties p = new MicaVoiceProperties();
		assertNotNull(p.getModelsDir());
		assertNotNull(p.getOutputDir());
		assertTrue(p.getThreads() > 0);
	}

	@Test
	void builder(@TempDir File tmp) {
		MicaVoiceProperties p = MicaVoiceProperties.builder()
			.modelsDir(tmp)
			.outputDir(new File(tmp, "out"))
			.threads(4)
			.debug(true)
			.build();
		assertEquals(tmp, p.getModelsDir());
		assertEquals(4, p.getThreads());
		assertTrue(p.isDebug());
		// ensureOutputDir 应自动创建
		File out = p.ensureOutputDir();
		assertTrue(out.isDirectory());
	}

	@Test
	void invalidThreads() {
		MicaVoiceProperties p = new MicaVoiceProperties();
		assertThrows(IllegalArgumentException.class, () -> p.setThreads(0));
		assertThrows(IllegalArgumentException.class, () -> p.setThreads(-1));
	}
}
