package com.mica.voice.example.console;

import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
import net.dreamlu.mica.voice.core.MicaVoice;

import java.io.File;

/**
 * X-ASR 中英流式 ASR（Zipformer2 Transducer，960ms chunk）。
 *
 * <p>模型：{@code x-asr-zh-en-chunk-960ms}（models/ 目录下载脚本 x-asr 目标）。
 * <br>测试音频：{@code 0-four-speakers-zh.wav}（四人对话，体验流式中间结果）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar xasr [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class OnlineAsrExample {

	public static final String MODEL_DIR = "x-asr-zh-en-chunk-960ms";
	public static final String DEFAULT_WAV = "0-four-speakers-zh.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		OnlineAsrConfig config = OnlineAsrConfig.builder()
			.modelDirName(MODEL_DIR)
			.modelType(OnlineAsrConfig.ModelType.X_ASR)
			.enableEndpoint(true)
			.chunkSize(1600)   // 16kHz * 100ms
			.build();

		try (OnlineAsrService svc = MicaVoice.onlineAsrTyped(props, config)) {
			AudioData audio = AudioReaders.read(wav);
			System.out.println("音频: " + wav.getAbsolutePath()
				+ "，时长约 " + (audio.getSamples().length / audio.getSampleRate()) + "s");
			System.out.println("流式识别中（100ms 分块喂入，打印中间结果）...\n");

			// 边喂边回调 partial，最后返回完整结果
			AsrResult finalResult = svc.recognizeStreaming(audio,
				partial -> System.out.println("  [中间] " + partial.getText()));
			ConsoleUtil.printAsr("X-ASR 流式识别（最终）", finalResult);
		}
	}
}
