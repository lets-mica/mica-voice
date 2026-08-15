package com.mica.voice.example.console;

import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.config.DiarizationConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.diarization.DiarizationSegment;
import net.dreamlu.mica.voice.diarization.DiarizationService;

import java.io.File;
import java.util.List;

/**
 * 说话人分离（Speaker Diarization）。
 *
 * <p>需要两个模型文件：
 * <ul>
 *     <li>{@code sherpa-onnx-pyannote-segmentation-3-0.onnx}（segmentation）</li>
 *     <li>{@code 3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx}（embedding，
 *         复用 speaker 脚本下载的声纹模型）</li>
 * </ul>
 *
 * <p>运行 {@code download-models.ps1 -Target diarization} 下载；embedding 模型与
 * speaker target 共用，若已运行过 speaker 则无需重复下载。
 *
 * <p>测试音频：{@code 0-four-speakers-zh.wav}（来自 speaker 脚本，4 人对话，最适合看分离效果）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar diarization [wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class DiarizationExample {

	public static final String DEFAULT_WAV = "0-four-speakers-zh.wav";

	public static void main(String[] args) {
		File wav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_WAV);

		MicaVoiceConfig props = MicaVoiceConfig.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		DiarizationConfig config = DiarizationConfig.builder()
			.numClusters(4)   // 测试音频是 4 人对话；0 = 自动推断
			.clusterThreshold(0.5f)
			.build();

		try (DiarizationService svc = MicaVoice.diarization(props, config)) {
			AudioData audio = AudioReaders.read(wav);
			System.out.println("音频: " + wav.getName()
				+ "（约 " + audio.getSamples().length / audio.getSampleRate() + "s）");

			List<DiarizationSegment> segs = svc.diarize(audio);
			System.out.println("==== 说话人分离结果 ====");
			if (segs.isEmpty()) {
				System.out.println("未检测到任何说话人片段。");
				return;
			}
			int prevSpeaker = -1;
			for (DiarizationSegment s : segs) {
				if (s.getSpeaker() != prevSpeaker) {
					System.out.println();
					System.out.printf("  [说话人 %d]%n", s.getSpeaker());
					prevSpeaker = s.getSpeaker();
				}
				System.out.printf("    %.2fs → %.2fs  (%.2fs)%n",
					s.getStartSec(), s.getEndSec(), s.getEndSec() - s.getStartSec());
			}
			System.out.println("\n共 " + segs.size() + " 段，涉及 "
				+ segs.stream().mapToInt(DiarizationSegment::getSpeaker).distinct().count() + " 位说话人。");
		}
	}
}