package com.mica.voice.example.console;

import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.MicaVoiceProperties;
import net.dreamlu.mica.voice.config.VadConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.vad.VadSegment;
import net.dreamlu.mica.voice.vad.VadService;

import java.io.File;
import java.util.List;

/**
 * VAD 语音活动检测（Silero VAD）。
 *
 * <p>模型：{@code silero_vad.onnx}（v1.1+；models/ 目录下载脚本暂未提供该 target，
 * 可从 <a href="https://github.com/snakers4/silero-vad">silero-vad</a> 官方仓库下载
 * 到 {@code models/silero_vad.onnx}，或自定义任意子目录 + 配置项 modelFileName 指向）。
 *
 * <p>测试音频：复用 Paraformer 模型自带 4 人对话音频（{@code 0-four-speakers-zh.wav}，
 * 来自 speaker 脚本），便于体验多人语音中的分段效果。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar vad [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class VadExample {

	public static final String MODEL_FILE = "silero_vad.onnx";
	public static final String DEFAULT_WAV = "0-four-speakers-zh.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceProperties props = MicaVoiceProperties.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		VadConfig config = VadConfig.builder()
			.modelFileName(MODEL_FILE)
			.threshold(0.5f)
			.build();

		try (VadService svc = MicaVoice.vad(props, config)) {
			AudioData audio = AudioReaders.read(wav);
			System.out.println("音频: " + wav.getName() + "（约 " + audio.getSamples().length / audio.getSampleRate() + "s）");

			List<VadSegment> segs = svc.detect(audio);
			System.out.println("==== VAD 检测结果 ====");
			if (segs.isEmpty()) {
				System.out.println("未检测到语音片段。");
				return;
			}
			for (int i = 0; i < segs.size(); i++) {
				VadSegment s = segs.get(i);
				float startSec = s.getStartSample() / (float) s.getSampleRate();
				float endSec = startSec + s.durationSeconds();
				System.out.printf("  #%d  [%.2fs → %.2fs]  时长 %.2fs%n",
					i + 1, startSec, endSec, s.durationSeconds());
			}
			System.out.println("共 " + segs.size() + " 段语音。");
		}
	}
}