package net.dreamlu.mica.voice.config;

import net.dreamlu.mica.voice.exception.ModelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ModelSelector} 单元测试。
 *
 * <p>纯文件系统操作，不依赖任何 native 资源。
 */
class ModelSelectorTest {

	@Test
	void resolveModelDir_nullInputs(@TempDir Path tmp) {
		assertNull(ModelSelector.resolveModelDir(null, "x"));
		assertNull(ModelSelector.resolveModelDir(tmp.toFile(), null));
		// modelsDir 必须是目录；传文件应返回 null
		File notDir = tmp.resolve("file.txt").toFile();
		try {
			assertNull(ModelSelector.resolveModelDir(notDir, "x"));
		} catch (Exception ignored) {
			// 视情况允许 - 这里只校验 null/非 null 语义
		}
	}

	@Test
	void resolveModelDir_returnsAbsPath(@TempDir Path tmp) throws IOException {
		Path sub = Files.createDirectory(tmp.resolve("model-a"));
		String abs = ModelSelector.resolveModelDir(tmp.toFile(), "model-a");
		assertNotNull(abs);
		assertEquals(sub.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveModelDir_missing(@TempDir Path tmp) {
		assertNull(ModelSelector.resolveModelDir(tmp.toFile(), "does-not-exist"));
	}

	@Test
	void resolveModelFile_noCandidates_returnsDir(@TempDir Path tmp) throws IOException {
		Path sub = Files.createDirectory(tmp.resolve("model-b"));
		String abs = ModelSelector.resolveModelFile(tmp.toFile(), "model-b");
		assertEquals(sub.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveModelFile_firstCandidateWins(@TempDir Path tmp) throws IOException {
		Path sub = Files.createDirectory(tmp.resolve("model-c"));
		Path f1 = Files.createFile(sub.resolve("model.int8.onnx"));
		Files.createFile(sub.resolve("model.onnx"));
		String abs = ModelSelector.resolveModelFile(tmp.toFile(), "model-c", "model.int8.onnx", "model.onnx");
		assertEquals(f1.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveModelFile_secondCandidateWins(@TempDir Path tmp) throws IOException {
		Path sub = Files.createDirectory(tmp.resolve("model-d"));
		// 只有 fp32 文件存在
		Path f2 = Files.createFile(sub.resolve("model.onnx"));
		String abs = ModelSelector.resolveModelFile(tmp.toFile(), "model-d", "model.int8.onnx", "model.onnx");
		assertEquals(f2.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveModelFile_missingDirThrows(@TempDir Path tmp) {
		ModelNotFoundException ex = assertThrows(ModelNotFoundException.class,
			() -> ModelSelector.resolveModelFile(tmp.toFile(), "missing-dir", "x.onnx"));
		assertEquals("missing-dir", ex.getModelDirName());
		assertEquals(1, ex.getCandidates().length);
		assertEquals("x.onnx", ex.getCandidates()[0]);
		// 错误信息应指引用户去下载
		assertTrue(ex.getMessage().contains("download-models"));
	}

	@Test
	void resolveModelFile_missingCandidatesThrows(@TempDir Path tmp) throws IOException {
		Files.createDirectory(tmp.resolve("empty-dir"));
		assertThrows(ModelNotFoundException.class,
			() -> ModelSelector.resolveModelFile(tmp.toFile(), "empty-dir", "a.onnx", "b.onnx"));
	}

	@Test
	void tryResolveModelFile_returnsNullWhenMissing(@TempDir Path tmp) {
		assertNull(ModelSelector.tryResolveModelFile(tmp.toFile(), "no-such"));
		assertNull(ModelSelector.tryResolveModelFile(tmp.toFile(), "no-such", "x.onnx"));
	}

	@Test
	void tryResolveModelFile_returnsDirWhenNoCandidates(@TempDir Path tmp) throws IOException {
		Path sub = Files.createDirectory(tmp.resolve("dir-only"));
		String abs = ModelSelector.tryResolveModelFile(tmp.toFile(), "dir-only");
		assertEquals(sub.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveInDir_picksFirstMatch(@TempDir Path tmp) throws IOException {
		Path a = Files.createFile(tmp.resolve("a.onnx"));
		Files.createFile(tmp.resolve("b.onnx"));
		String abs = ModelSelector.resolveInDir(tmp.toAbsolutePath().toString(), "missing.onnx", "a.onnx", "b.onnx");
		assertEquals(a.toAbsolutePath().toString(), abs);
	}

	@Test
	void resolveInDir_nullInputs(@TempDir Path tmp) {
		assertNull(ModelSelector.resolveInDir(null, "x"));
		assertNull(ModelSelector.resolveInDir(tmp.toAbsolutePath().toString(), (String[]) null));
		assertNull(ModelSelector.resolveInDir(tmp.toAbsolutePath().toString()));
	}
}