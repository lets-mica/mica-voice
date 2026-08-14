package com.mica.voice.example.console;

import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;

import java.io.File;

/**
 * TTS 语音合成（VITS Melo 中英混合）。
 *
 * <p>模型：{@code vits-melo-tts-zh_en}（models/ 目录下载脚本 tts-zh-en 目标），
 * 支持中文与英文混合输入。纯中文也可改回 {@code vits-icefall-zh-aishell3}。
 * <br>合成结果保存为 {@code output/tts-output.wav}（16-bit PCM WAV）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar tts [文本] [说话人id]
 * </pre>
 *
 * @author dreamlu
 */
public class TtsExample {

	public static final String MODEL_DIR = "vits-melo-tts-zh_en";
	public static final String DEFAULT_TEXT = "大家好，我是 mica-voice，一个开源的 Java 声音 AI 全家桶。";

	public static void main(String[] args) {
		String text = args.length > 0 ? args[0] : DEFAULT_TEXT;
		int sid = args.length > 1 ? Integer.parseInt(args[1]) : 0;

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.outputDir("output")
			.threads(2)
			.build();
		TtsConfig config = TtsConfig.builder()
			.modelDirName(MODEL_DIR)
			.build();

		try (TtsService svc = MicaVoice.tts(props, config)) {
			System.out.println("模型说话人数: " + svc.getNumSpeakers() + "，采样率: " + svc.getSampleRate() + " Hz");

			TtsAudio audio = svc.synthesize(text, sid, 1.0f);
			File out = ConsoleUtil.writeTtsWav(props, audio, "tts-output.wav");

			System.out.println("文本: " + text);
			System.out.println("时长: " + String.format("%.2f", audio.durationSeconds()) + "s"
				+ "，耗时: " + audio.getCostMs() + " ms");
			System.out.println("输出: " + out.getAbsolutePath());
		}
	}
}
