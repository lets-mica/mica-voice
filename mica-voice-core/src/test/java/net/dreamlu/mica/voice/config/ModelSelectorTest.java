package net.dreamlu.mica.voice.config;

import net.dreamlu.mica.voice.exception.ModelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ModelSelectorTest {

	@Test
	void resolveModelDir(@TempDir File tmp) throws IOException {
		File sub = new File(tmp, "model-a");
		assertTrue(sub.mkdir());
		assertEquals(sub.getAbsolutePath(),
			ModelSelector.resolveModelDir(tmp, "model-a"));
		assertNull(ModelSelector.resolveModelDir(tmp, "not-exist"));
	}

	@Test
	void resolveModelFile_int8First(@TempDir File tmp) throws IOException {
		File sub = new File(tmp, "model-a");
		assertTrue(sub.mkdir());
		Files.write(new File(sub, "tokens.txt").toPath(), new byte[]{1});
		Files.write(new File(sub, "model.onnx").toPath(), new byte[]{1});
		Files.write(new File(sub, "model.int8.onnx").toPath(), new byte[]{1});

		String path = ModelSelector.resolveModelFile(tmp, "model-a",
			"model.int8.onnx", "model.onnx");
		assertTrue(path.endsWith("model.int8.onnx"));
	}

	@Test
	void resolveModelFile_fallbackFp32(@TempDir File tmp) throws IOException {
		File sub = new File(tmp, "model-a");
		assertTrue(sub.mkdir());
		Files.write(new File(sub, "model.onnx").toPath(), new byte[]{1});

		String path = ModelSelector.resolveModelFile(tmp, "model-a",
			"model.int8.onnx", "model.onnx");
		assertTrue(path.endsWith("model.onnx"));
	}

	@Test
	void resolveModelFile_missingDir(@TempDir File tmp) {
		assertThrows(ModelNotFoundException.class,
			() -> ModelSelector.resolveModelFile(tmp, "not-exist", "x.onnx"));
	}

	@Test
	void resolveModelFile_missingFile(@TempDir File tmp) throws IOException {
		File sub = new File(tmp, "model-a");
		assertTrue(sub.mkdir());
		assertThrows(ModelNotFoundException.class,
			() -> ModelSelector.resolveModelFile(tmp, "model-a", "x.onnx"));
	}

	@Test
	void tryResolveModelFile(@TempDir File tmp) {
		assertNull(ModelSelector.tryResolveModelFile(tmp, "not-exist", "x.onnx"));
	}
}
