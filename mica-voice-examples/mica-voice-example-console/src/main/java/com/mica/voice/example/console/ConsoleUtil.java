package com.mica.voice.example.console;

import com.k2fsa.sherpa.onnx.WaveWriter;
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.config.MicaVoiceProperties;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.util.Models;

import java.io.File;

/**
 * 控制台示例公共工具。
 *
 * <p>模型目录解析复用 {@link Models#modelsDir()}，支持系统属性覆盖：
 * <ul>
 *     <li>{@code -Dmica.voice.models-dir=E:/.../models}（mica-voice 自身属性）</li>
 *     <li>{@code -Dsherpa.onnx.models.dir=E:/.../models}（兼容旧 demo 属性）</li>
 * </ul>
 * 不设置时默认取当前工作目录下的 {@code models}（在仓库根目录运行即可命中根 models/）。
 *
 * @author dreamlu
 */
public final class ConsoleUtil {

	private ConsoleUtil() {
	}

	/**
	 * 模型根目录（默认 ./models）。
	 */
	public static File modelsDir() {
		return Models.modelsDir();
	}

	/**
	 * 解析 wav 路径：绝对路径直接用；相对路径视为相对模型根目录。
	 */
	public static File resolve(String path) {
		File f = new File(path);
		if (f.isAbsolute()) {
			return f;
		}
		return new File(modelsDir(), path);
	}

	/**
	 * 打印 ASR 结果（含 SenseVoice 的语言/情感/事件元信息）。
	 */
	public static void printAsr(String title, AsrResult r) {
		System.out.println("==== " + title + " ====");
		System.out.println("文本: " + r.getText());
		if (r.getLanguage() != null) {
			System.out.println("语言: " + r.getLanguage());
		}
		if (r.getEmotion() != null) {
			System.out.println("情感: " + r.getEmotion());
		}
		if (r.getEvent() != null) {
			System.out.println("事件: " + r.getEvent());
		}
		System.out.println("耗时: " + r.getCostMs() + " ms");
		System.out.println();
	}

	/**
	 * 把 TTS 结果写为 wav 文件到输出目录。
	 */
	public static File writeTtsWav(MicaVoiceProperties props, TtsAudio audio, String name) {
		File out = new File(props.ensureOutputDir(), name);
		if (!WaveWriter.write(out.getAbsolutePath(), audio.getSamples(), audio.getSampleRate())) {
			throw new IllegalStateException("写入 wav 失败: " + out.getAbsolutePath());
		}
		return out;
	}
}
