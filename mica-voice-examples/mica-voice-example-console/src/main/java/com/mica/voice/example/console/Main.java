package com.mica.voice.example.console;

/**
 * mica-voice 控制台示例统一入口。
 *
 * <p>打 fat jar 后可直接运行（模型默认取仓库根 {@code models/} 目录，
 * 可用 {@code -Dmica.voice.models-dir=E:/.../models} 覆盖）：
 * <pre>
 *   java -jar mica-voice-example-console.jar asr           # 离线 ASR（Paraformer）
 *   java -jar mica-voice-example-console.jar sensevoice    # SenseVoice 多语言 ASR
 *   java -jar mica-voice-example-console.jar xasr          # X-ASR 流式 ASR
 *   java -jar mica-voice-example-console.jar tts           # TTS 合成
 *   java -jar mica-voice-example-console.jar speaker       # 声纹注册/验证/搜索
 *   java -jar mica-voice-example-console.jar vad           # VAD 语音活动检测（v1.1+）
 *   java -jar mica-voice-example-console.jar diarization   # 说话人分离（v1.1+）
 *   java -jar mica-voice-example-console.jar kws           # 关键词唤醒（v1.1+）
 *   java -jar mica-voice-example-console.jar denoise       # 音频降噪（v1.1+）
 *   java -jar mica-voice-example-console.jar all           # 依次跑全部示例
 * </pre>
 *
 * @author dreamlu
 */
public final class Main {

	private Main() {
	}

	public static void main(String[] args) {
		String cmd = args.length > 0 ? args[0] : "help";
		switch (cmd) {
			case "asr":
				AsrExample.main(shift(args));
				break;
			case "sensevoice":
				SenseVoiceExample.main(shift(args));
				break;
			case "xasr":
				OnlineAsrExample.main(shift(args));
				break;
			case "tts":
				TtsExample.main(shift(args));
				break;
			case "speaker":
				SpeakerExample.main(shift(args));
				break;
			case "vad":
				VadExample.main(shift(args));
				break;
			case "diarization":
				DiarizationExample.main(shift(args));
				break;
			case "kws":
				KwsExample.main(shift(args));
				break;
			case "denoise":
				DenoiseExample.main(shift(args));
				break;
			case "all":
				all();
				break;
			default:
				help();
		}
	}

	/**
	 * 依次运行全部示例（每个示例独立创建/释放 native 资源）。
	 * v1.1+ 的 vad/diarization/kws/denoise 模型未默认下载，会快速失败提示，可单独跑其它命令。
	 */
	private static void all() {
		AsrExample.main(new String[0]);
		SenseVoiceExample.main(new String[0]);
		OnlineAsrExample.main(new String[0]);
		TtsExample.main(new String[0]);
		SpeakerExample.main(new String[0]);
	}

	private static String[] shift(String[] args) {
		String[] rest = new String[args.length - 1];
		System.arraycopy(args, 1, rest, 0, rest.length);
		return rest;
	}

	private static void help() {
		System.out.println("mica-voice 控制台示例（基于 mica-voice-core）");
		System.out.println();
		System.out.println("用法: java -jar mica-voice-example-console.jar <命令> [参数]");
		System.out.println();
		System.out.println("命令:");
		System.out.println("  asr [wav]              离线 ASR（Paraformer 中文，默认 test_wavs/2-zh-en.wav）");
		System.out.println("  sensevoice [wav] [语言] SenseVoice 多语言 ASR（语言: auto/zh/en/ja/ko/yue）");
		System.out.println("  xasr [wav]             X-ASR 中英流式 ASR（默认 0-four-speakers-zh.wav）");
		System.out.println("  tts [文本] [sid]       TTS 合成（默认输出 output/tts-output.wav）");
		System.out.println("  speaker [wav]          声纹注册/验证/搜索（默认搜索 0-four-speakers-zh.wav）");
		System.out.println("  vad [wav]              VAD 语音活动检测（v1.1+，需要 silero_vad.onnx）");
		System.out.println("  diarization [wav]      说话人分离（v1.1+，需要 segmentation 模型）");
		System.out.println("  kws [wav]              关键词唤醒（v1.1+，需要 kws 模型 + keywords.txt）");
		System.out.println("  denoise [wav]          音频降噪（v1.1+，需要 gtcrn 模型，默认输出 output/denoise-output.wav）");
		System.out.println("  all                    依次运行全部示例");
		System.out.println("  help                   显示本帮助");
		System.out.println();
		System.out.println("模型目录: 默认 ./models，可用 -Dmica.voice.models-dir=绝对路径 覆盖");
		System.out.println("          模型下载: bash models/scripts/download-models.sh（Windows: models\\scripts\\download-models.bat）");
	}
}
