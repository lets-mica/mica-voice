package com.mica.voice.example.console;

import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceProperties;
import net.dreamlu.mica.voice.core.MicaVoice;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SenseVoice 多语言离线 ASR（中/英/日/韩/粤 + 情感 + 音频事件）。
 *
 * <p>模型：{@code sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17}（models/ 目录下载脚本 sensevoice 目标）。
 * <br>测试音频：模型自带 {@code test_wavs/zh.wav}（另有 en/ja/ko/yue.wav）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar sensevoice [wav路径] [语言]
 * </pre>
 * 语言：auto / zh / en / ja / ko / yue（默认 auto，自动检测）。
 *
 * @author dreamlu
 */
public class SenseVoiceExample {

	public static final String MODEL_DIR = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17";
	public static final String DEFAULT_WAV = MODEL_DIR + "/test_wavs/zh.wav";
	private static final Set<String> LANGS = new HashSet<>(Arrays.asList("auto", "zh", "en", "ja", "ko", "yue"));

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);
		String lang = args.length > 1 ? args[1] : "auto";
		if (!LANGS.contains(lang)) {
			System.err.println("语言参数只能是 auto/zh/en/ja/ko/yue，当前: " + lang);
			return;
		}

		MicaVoiceProperties props = MicaVoiceProperties.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		AsrConfig config = AsrConfig.builder()
			.modelDirName(MODEL_DIR)
			.modelType(AsrConfig.ModelType.SENSE_VOICE)
			.language(lang)
			.inverseTextNormalization(true)
			.build();

		try (AsrService svc = MicaVoice.asr(props, config)) {
			AsrResult result = svc.recognize(wav);
			ConsoleUtil.printAsr("SenseVoice 多语言 ASR（language=" + lang + "）", result);
		}
	}
}
