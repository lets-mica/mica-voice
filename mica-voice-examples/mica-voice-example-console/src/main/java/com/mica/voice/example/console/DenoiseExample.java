package com.mica.voice.example.console;

import com.k2fsa.sherpa.onnx.WaveWriter;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.DenoiseConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.denoise.DenoiseService;
import net.dreamlu.mica.voice.denoise.DenoisedAudio;

import java.io.File;

/**
 * 音频降噪（Denoise，GTCRN / DPDFNet）。
 *
 * <p>模型：{@code sherpa-onnx-gtcrn.onnx}（运行 {@code download-models.ps1 -Target denoise} 下载，
 * 或从 <a href="https://github.com/k2fsa/sherpa-onnx/releases/tag/speech-enhancement-models">speech-enhancement-models release</a>
 * 下载到 {@code models/} 根目录）。
 *
 * <p>测试音频：{@code 0-four-speakers-zh.wav}（任意 wav 都可；这里用它便于 demo）。
 * 输出：降噪后的 wav 写到 {@code output/denoise-output.wav}。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar denoise [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class DenoiseExample {

	public static final String MODEL_FILE = "sherpa-onnx-gtcrn.onnx";
	public static final String DEFAULT_WAV = "0-four-speakers-zh.wav";
	public static final String OUTPUT_FILE = "denoise-output.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.outputDir("output")
			.threads(2)
			.build();
		DenoiseConfig config = DenoiseConfig.builder()
			.modelFileName(MODEL_FILE)
			.modelType(DenoiseConfig.ModelType.GTCRN)
			.build();

		try (DenoiseService svc = MicaVoice.denoise(props, config)) {
			AudioData audio = AudioReaders.read(wav);
			System.out.println("输入: " + wav.getName()
				+ "（约 " + audio.getSamples().length / audio.getSampleRate() + "s）");

			DenoisedAudio out = svc.denoise(audio);
			System.out.println("==== 降噪完成 ====");
			System.out.println("输出采样率: " + out.getSampleRate() + " Hz");
			System.out.println("输出时长: " + String.format("%.2f", out.durationSeconds()) + "s");
			System.out.println("耗时: " + out.getCostMs() + " ms");

			File outFile = new File(props.ensureOutputDir(), OUTPUT_FILE);
			if (!WaveWriter.write(outFile.getAbsolutePath(), out.getSamples(), out.getSampleRate())) {
				throw new IllegalStateException("写入 wav 失败: " + outFile.getAbsolutePath());
			}
			System.out.println("已写入: " + outFile.getAbsolutePath());
		}
	}
}