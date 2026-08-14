package com.mica.voice.example.console;

import net.dreamlu.mica.voice.config.MicaVoiceProperties;
import net.dreamlu.mica.voice.config.SpeakerConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.speaker.SearchResult;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.speaker.VerificationResult;

import java.io.File;

/**
 * 声纹识别（说话人注册 / 1:1 验证 / 1:N 搜索）。
 *
 * <p>模型：{@code 3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx}（models/ 目录下载脚本 speaker 目标）。
 * <br>注册音频：Paraformer 模型自带的方言测试音频，确认来自不同说话人：
 * {@code test_wavs/3-sichuan.wav}（四川话）、{@code test_wavs/4-tianjin.wav}（天津话）。
 * <br>测试音频：{@code test_wavs/5-henan.wav}（河南话，第三人，预期搜索无匹配）。
 *
 * <p>运行（仓库根目录）：
 * <pre>
 *   java -jar mica-voice-example-console.jar speaker [测试wav路径]
 * </pre>
 *
 * @author dreamlu
 */
public class SpeakerExample {

	public static final String DEFAULT_SEARCH_WAV = "sherpa-onnx-paraformer-zh-small-2024-03-09/test_wavs/5-henan.wav";
	public static final String WAV_A = "sherpa-onnx-paraformer-zh-small-2024-03-09/test_wavs/3-sichuan.wav";
	public static final String WAV_B = "sherpa-onnx-paraformer-zh-small-2024-03-09/test_wavs/4-tianjin.wav";

	public static void main(String[] args) {
		File searchWav = ConsoleUtil.resolve(args.length > 0 ? args[0] : DEFAULT_SEARCH_WAV);

		MicaVoiceProperties props = MicaVoiceProperties.builder()
			.modelsDir(ConsoleUtil.modelsDir())
			.threads(2)
			.build();
		SpeakerConfig config = SpeakerConfig.builder()
			.threshold(0.5f)
			.build();

		try (SpeakerService svc = MicaVoice.speaker(props, config)) {
			// 1. 注册两个不同说话人（四川话 / 天津话）
			svc.enroll("speaker-a", ConsoleUtil.resolve(WAV_A));
			svc.enroll("speaker-b", ConsoleUtil.resolve(WAV_B));
			System.out.println("已注册说话人: " + svc.names() + "\n");

			// 2. 1:1 验证：同人应匹配，不同人应不匹配
			verify(svc, "speaker-a", ConsoleUtil.resolve(WAV_A), true);
			verify(svc, "speaker-a", ConsoleUtil.resolve(WAV_B), false);

			// 3. 1:N 搜索：河南话为第三人，预期无匹配
			SearchResult r = svc.search(searchWav);
			System.out.println("搜索 " + searchWav.getName() + " → 匹配: "
				+ (r.isMatched() ? r.getSpeakerName() : "无（未超过阈值）")
				+ "（score=" + r.getScore() + ", threshold=" + r.getThreshold() + "）");
		}
	}

	private static void verify(SpeakerService svc, String name, File wav, boolean expectMatched) {
		VerificationResult v = svc.verify(name, wav);
		String mark = v.isMatched() == expectMatched ? "" : " [与预期不符!]";
		System.out.println("验证 " + name + " vs " + wav.getName() + " → "
			+ (v.isMatched() ? "匹配" : "不匹配")
			+ "（score=" + v.getScore() + ", threshold=" + v.getThreshold() + "）" + mark);
	}
}
