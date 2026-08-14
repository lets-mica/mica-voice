package net.dreamlu.mica.voice.util;

import lombok.experimental.UtilityClass;
import net.dreamlu.mica.voice.config.MicaVoiceProperties;

import java.io.File;

/**
 * mica-voice 模型/输出路径解析工具。
 *
 * <p>与 {@link MicaVoiceProperties} 配合使用：
 * <pre>
 *   File modelsDir = Models.modelsDir();      // 默认 ./models，可用 -Dsherpa.onnx.models.dir 覆盖
 *   File outputDir = Models.outputDir();      // 默认 ./output，自动创建
 * </pre>
 *
 * <p>兼容两个系统属性：{@code sherpa.onnx.models.dir}（历史遗留）和 {@code mica.voice.models-dir}（推荐）。
 *
 * @author dreamlu
 */
@UtilityClass
public class Models {

	/**
	 * 兼容旧 demo 的系统属性名
	 */
	public final String SYS_MODELS_DIR_COMPAT = "sherpa.onnx.models.dir";
	/**
	 * mica-voice 自身的系统属性名
	 */
	public final String SYS_MODELS_DIR = MicaVoiceProperties.SYS_MODELS_DIR;

	/**
	 * 解析模型根目录。优先 {@code mica.voice.models-dir}，其次 {@code sherpa.onnx.models.dir}，最后默认 {@code models}。
	 */
	public File modelsDir() {
		String dir = System.getProperty(SYS_MODELS_DIR);
		if (dir == null || dir.isEmpty()) {
			dir = System.getProperty(SYS_MODELS_DIR_COMPAT, "models");
		}
		return new File(dir);
	}

	/**
	 * 解析输出目录。优先 {@code mica.voice.output-dir}，最后默认 {@code output}（自动创建）。
	 */
	public File outputDir() {
		String dir = System.getProperty("mica.voice.output-dir", "output");
		File f = new File(dir);
		if (!f.exists()) {
			//noinspection ResultOfMethodCallIgnored
			f.mkdirs();
		}
		return f;
	}
}
