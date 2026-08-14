package net.dreamlu.mica.voice.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Models} 系统属性解析工具测试。
 *
 * <p>用 {@link AfterEach} 清理设置的 system property，避免污染其他测试。
 */
class ModelsTest {

	@AfterEach
	void clearSysProps() {
		System.clearProperty(Models.SYS_MODELS_DIR);
		System.clearProperty(Models.SYS_MODELS_DIR_COMPAT);
		System.clearProperty("mica.voice.output-dir");
	}

	@Test
	void modelsDir_defaultWhenNoProp(@TempDir File tmp) {
		// 把 cwd 切换到一个明确目录，避免被外部干扰
		System.clearProperty(Models.SYS_MODELS_DIR);
		System.clearProperty(Models.SYS_MODELS_DIR_COMPAT);
		File got = Models.modelsDir();
		assertEquals(new File("models"), got);
	}

	@Test
	void modelsDir_prefersMicaProperty(@TempDir File tmp) {
		System.setProperty(Models.SYS_MODELS_DIR, tmp.getAbsolutePath());
		System.setProperty(Models.SYS_MODELS_DIR_COMPAT, "/should-be-ignored");
		assertEquals(new File(tmp.getAbsolutePath()), Models.modelsDir());
	}

	@Test
	void modelsDir_fallsBackToCompat(@TempDir File tmp) {
		System.clearProperty(Models.SYS_MODELS_DIR);
		System.setProperty(Models.SYS_MODELS_DIR_COMPAT, tmp.getAbsolutePath());
		assertEquals(new File(tmp.getAbsolutePath()), Models.modelsDir());
	}

	@Test
	void outputDir_createsWhenMissing(@TempDir File tmp) {
		File out = new File(tmp, "nested/output");
		System.setProperty("mica.voice.output-dir", out.getAbsolutePath());
		File got = Models.outputDir();
		assertEquals(out, got);
		assertTrue(got.isDirectory(), "outputDir should be auto-created");
	}

	@Test
	void outputDir_defaultName() {
		System.clearProperty("mica.voice.output-dir");
		File got = Models.outputDir();
		assertEquals(new File("output"), got);
		// 默认应该也被自动创建
		assertTrue(got.isDirectory());
	}
}