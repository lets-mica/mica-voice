package com.mica.voice.example.console;

import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;

import java.io.File;

/**
 * 离线 ASR（Paraformer 中文/英文）。
 *
 * <p>模型：{@code sherpa-onnx-paraformer-zh-small-2024-03-09}（models/ 目录下载脚本 asr 目标）。
 * <br>测试音频：模型自带 {@code test_wavs/2-zh-en.wav}（中英混合）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar asr [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class AsrExample {

	public static final String MODEL_DIR = "sherpa-onnx-paraformer-zh-small-2024-03-09";
	public static final String DEFAULT_WAV = MODEL_DIR + "/test_wavs/2-zh-en.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		AsrConfig config = AsrConfig.builder()
			.modelDirName(MODEL_DIR)
			.modelType(AsrConfig.ModelType.PARAFORMER)
			.build();

		try (AsrService svc = MicaVoice.asr(props, config)) {
			AsrResult result = svc.recognize(wav);
			ConsoleUtil.printAsr("离线 ASR（Paraformer）", result);
		}
	}
}
