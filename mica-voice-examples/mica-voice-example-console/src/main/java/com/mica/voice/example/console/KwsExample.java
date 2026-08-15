package com.mica.voice.example.console;

import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.KwsConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.kws.KwsResult;
import net.dreamlu.mica.voice.kws.KwsService;

import java.io.File;
import java.util.List;

/**
 * 关键词唤醒（KWS / Keyword Spotting）。
 *
 * <p>模型目录：{@code sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23/}
 * （运行 {@code download-models.ps1 -Target kws} 下载，或从
 * <a href="https://github.com/k2-fsa/sherpa-onnx/releases/tag/kws-models">kws-models release</a>
 * 下载到 {@code models/} 根目录，并确保目录里有 {@code keywords.txt}）。
 *
 * <p>测试音频：{@code 0-four-speakers-zh.wav}（任意带语音的 wav 都能用，只要关键词在 keywords.txt 中）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar kws [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class KwsExample {

	public static final String MODEL_DIR = "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23";
	public static final String DEFAULT_WAV = "0-four-speakers-zh.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		KwsConfig config = KwsConfig.builder()
			.modelDirName(MODEL_DIR)
			.keywordsThreshold(0.25f)
			.keywordsScore(2.0f)
			.build();

		try (KwsService svc = MicaVoice.kws(props, config)) {
			AudioData audio = AudioReaders.read(wav);
			System.out.println("音频: " + wav.getName()
				+ "（约 " + audio.getSamples().length / audio.getSampleRate() + "s）");

			List<KwsResult> hits = svc.spot(audio);
			System.out.println("==== 关键词识别结果 ====");
			if (hits.isEmpty()) {
				System.out.println("未命中任何关键词。");
				return;
			}
			for (int i = 0; i < hits.size(); i++) {
				KwsResult r = hits.get(i);
				float sec = r.getTriggeredAtSample() / 16000.0f;
				System.out.printf("  #%d  %.2fs  keyword=%s  tokens=%s%n",
					i + 1, sec, r.getKeyword(), r.getTokens());
			}
			System.out.println("\n共 " + hits.size() + " 次命中。");
		}
	}
}