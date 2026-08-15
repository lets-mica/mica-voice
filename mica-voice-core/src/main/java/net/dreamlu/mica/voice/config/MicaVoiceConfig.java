package net.dreamlu.mica.voice.config;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * mica-voice 全局配置。
 *
 * <p>所有路径和运行时参数集中管理。默认行为：
 * <ul>
 *     <li>模型根目录：{@code ./models}（可用系统属性 {@code mica.voice.models-dir} 覆盖）</li>
 *     <li>输出目录：{@code ./output}（不存在自动创建）</li>
 *     <li>线程数：2</li>
 *     <li>debug：false</li>
 * </ul>
 *
 * <p>各能力（ASR / TTS / Speaker / VAD）通过嵌套配置进一步细化。
 *
 * @author dreamlu
 */
@Getter
@Setter
public class MicaVoiceConfig {

	/**
	 * 系统属性：模型根目录
	 */
	public static final String SYS_MODELS_DIR = "mica.voice.models-dir";

	/**
	 * 模型根目录（默认 {@code ./models}，可用系统属性 {@code mica.voice.models-dir} 覆盖）。
	 */
	private File modelsDir;
	/**
	 * 输出目录（默认 {@code ./output}，ensureOutputDir 时按需创建）。
	 */
	private File outputDir;
	/**
	 * 默认推理线程数。
	 */
	private int threads;
	/**
	 * 是否输出 sherpa-onnx 调试日志。
	 */
	private boolean debug;

	/**
	 * 使用默认配置构造：模型根目录来自系统属性 {@code mica.voice.models-dir}（默认 {@code models}），
	 * 输出目录 {@code ./output}，线程数 2，debug = false。
	 */
	public MicaVoiceConfig() {
		String dir = System.getProperty(SYS_MODELS_DIR, "models");
		this.modelsDir = new File(dir);
		this.outputDir = new File("output");
		this.threads = 2;
		this.debug = false;
	}

	/**
	 * 创建一个带有自定义模型/输出目录的 Builder。
	 *
	 * @return Builder 实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 设置线程数。必须 &gt; 0。
	 *
	 * @param threads 推理线程数
	 * @throws IllegalArgumentException 当 threads &lt;= 0 时
	 */
	public void setThreads(int threads) {
		if (threads <= 0) {
			throw new IllegalArgumentException("threads must be > 0");
		}
		this.threads = threads;
	}

	/**
	 * 确保输出目录存在。
	 *
	 * @return 输出目录对象
	 * @throws IllegalStateException 当输出目录不存在且无法创建时
	 */
	public File ensureOutputDir() {
		if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
			throw new IllegalStateException("无法创建输出目录: " + outputDir.getAbsolutePath());
		}
		return outputDir;
	}

	/**
	 * Builder 形式（fluent API）。
	 */
	public static final class Builder {
		private final MicaVoiceConfig p = new MicaVoiceConfig();

		public Builder modelsDir(File dir) {
			p.modelsDir = dir;
			return this;
		}

		public Builder modelsDir(String dir) {
			return modelsDir(new File(dir));
		}

		public Builder outputDir(File dir) {
			p.outputDir = dir;
			return this;
		}

		public Builder outputDir(String dir) {
			return outputDir(new File(dir));
		}

		public Builder threads(int threads) {
			p.threads = threads;
			return this;
		}

		public Builder debug(boolean debug) {
			p.debug = debug;
			return this;
		}

		public MicaVoiceConfig build() {
			return p;
		}
	}
}
