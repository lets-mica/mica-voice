package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MicaVoiceConfig} Builder / 校验测试。
 */
class MicaVoiceConfigTest {

	@AfterEach
	void clearSysProps() {
		System.clearProperty(MicaVoiceConfig.SYS_MODELS_DIR);
	}

	@Test
	void defaultsFromConstructor() {
		MicaVoiceConfig p = new MicaVoiceConfig();
		assertEquals(new File("models"), p.getModelsDir());
		assertEquals(new File("output"), p.getOutputDir());
		assertEquals(2, p.getThreads());
		assertFalse(p.isDebug());
	}

	@Test
	void sysPropOverridesModelsDir(@TempDir File tmp) {
		System.setProperty(MicaVoiceConfig.SYS_MODELS_DIR, tmp.getAbsolutePath());
		MicaVoiceConfig p = new MicaVoiceConfig();
		assertEquals(tmp, p.getModelsDir());
	}

	@Test
	void builderChains() {
		MicaVoiceConfig p = MicaVoiceConfig.builder()
			.modelsDir("/tmp/m1")
			.outputDir("/tmp/o1")
			.threads(4)
			.debug(true)
			.build();
		assertEquals(new File("/tmp/m1"), p.getModelsDir());
		assertEquals(new File("/tmp/o1"), p.getOutputDir());
		assertEquals(4, p.getThreads());
		assertTrue(p.isDebug());
	}

	@Test
	void threadsMustBePositive() {
		MicaVoiceConfig p = new MicaVoiceConfig();
		assertThrows(IllegalArgumentException.class, () -> p.setThreads(0));
		assertThrows(IllegalArgumentException.class, () -> p.setThreads(-1));
	}

	@Test
	void ensureOutputDir_createsDir(@TempDir File tmp) {
		MicaVoiceConfig p = MicaVoiceConfig.builder()
			.outputDir(new File(tmp, "nested/out"))
			.build();
		File got = p.ensureOutputDir();
		assertTrue(got.isDirectory());
		assertTrue(got.exists());
	}
}