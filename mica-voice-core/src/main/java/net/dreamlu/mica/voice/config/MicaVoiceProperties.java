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
public class MicaVoiceProperties {

	/**
	 * 系统属性：模型根目录
	 */
	public static final String SYS_MODELS_DIR = "mica.voice.models-dir";

	private File modelsDir;
	private File outputDir;
	private int threads;
	private boolean debug;

	public MicaVoiceProperties() {
		String dir = System.getProperty(SYS_MODELS_DIR, "models");
		this.modelsDir = new File(dir);
		this.outputDir = new File("output");
		this.threads = 2;
		this.debug = false;
	}

	/**
	 * 创建一个带有自定义模型/输出目录的 Builder。
	 */
	public static Builder builder() {
		return new Builder();
	}

	public void setThreads(int threads) {
		if (threads <= 0) {
			throw new IllegalArgumentException("threads must be > 0");
		}
		this.threads = threads;
	}

	/**
	 * 确保输出目录存在。
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
		private final MicaVoiceProperties p = new MicaVoiceProperties();

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

		public MicaVoiceProperties build() {
			return p;
		}
	}
}
