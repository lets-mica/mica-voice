package net.dreamlu.mica.voice.core;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.asr.OfflineAsrService;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.config.*;
import net.dreamlu.mica.voice.denoise.DenoiseService;
import net.dreamlu.mica.voice.denoise.OfflineSpeechDenoiserService;
import net.dreamlu.mica.voice.diarization.DiarizationService;
import net.dreamlu.mica.voice.diarization.PyannoteDiarizationService;
import net.dreamlu.mica.voice.kws.KeywordSpotterService;
import net.dreamlu.mica.voice.kws.KwsService;
import net.dreamlu.mica.voice.speaker.SpeakerEmbeddingService;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.tts.OfflineTtsService;
import net.dreamlu.mica.voice.tts.TtsService;
import net.dreamlu.mica.voice.transcribe.OfflineDiarizationTranscribeService;
import net.dreamlu.mica.voice.vad.SileroVadService;
import net.dreamlu.mica.voice.vad.VadService;

import java.util.Objects;

/**
 * mica-voice 统一门面（静态工厂风格）。
 *
 * <p>位于 {@code net.dreamlu.mica.voice.core} 包，作为 mica-voice 核心能力的"一级"入口。
 *
 * <p>v1.0：asr / onlineAsr / tts / speaker
 * <br>v1.1：vad / diarization / kws / denoise
 *
 * <p>门面返回的服务对象都实现了 {@link AutoCloseable}，强烈建议用 try-with-resources。
 *
 * @author dreamlu
 */
@Slf4j
public final class MicaVoice {

	private MicaVoice() {
	}

	// =========================== v1.0 ===========================

	/**
	 * 离线 ASR。
	 */
	public static AsrService asr(MicaVoiceProperties props, AsrConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineAsrService: modelDir={}", config.getModelDirName());
		return new OfflineAsrService(props, config);
	}

	/**
	 * 在线流式 ASR（{@link AsrService} 形态）。
	 */
	public static AsrService onlineAsr(MicaVoiceProperties props, OnlineAsrConfig config) {
		return onlineAsrTyped(props, config);
	}

	/**
	 * 在线流式 ASR（强类型，便于用 createStream 等扩展方法）。
	 */
	public static OnlineAsrService onlineAsrTyped(MicaVoiceProperties props, OnlineAsrConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OnlineAsrService: modelDir={}", config.getModelDirName());
		return new OnlineAsrService(props, config);
	}

	/**
	 * TTS。
	 */
	public static TtsService tts(MicaVoiceProperties props, TtsConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineTtsService: modelDir={}", config.getModelDirName());
		return new OfflineTtsService(props, config);
	}

	/**
	 * 声纹识别。
	 */
	public static SpeakerService speaker(MicaVoiceProperties props, SpeakerConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 SpeakerEmbeddingService");
		return new SpeakerEmbeddingService(props, config);
	}

	// =========================== v1.1 ===========================

	/**
	 * VAD（语音活动检测）。
	 */
	public static VadService vad(MicaVoiceProperties props, VadConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 SileroVadService: model={}", config.getModelFileName());
		return new SileroVadService(props, config);
	}

	/**
	 * 说话人分离。
	 */
	public static DiarizationService diarization(MicaVoiceProperties props, DiarizationConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 PyannoteDiarizationService");
		return new PyannoteDiarizationService(props, config);
	}

	/**
	 * 关键词唤醒。
	 */
	public static KwsService kws(MicaVoiceProperties props, KwsConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 KeywordSpotterService: modelDir={}", config.getModelDirName());
		return new KeywordSpotterService(props, config);
	}

	/**
	 * 音频降噪。
	 */
	public static DenoiseService denoise(MicaVoiceProperties props, DenoiseConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineSpeechDenoiserService: model={}", config.getModelFileName());
		return new OfflineSpeechDenoiserService(props, config);
	}

	/**
	 * "说话人分离 + 转写"联合服务（v1.1+）。
	 *
	 * <p>传入 DiarizationService + AsrService（通常都使用 mica-voice 装配的离线版本），
	 * 返回组合后的转写结果：每个说话人在某段时间的文本。
	 */
	public static OfflineDiarizationTranscribeService transcribe(
			DiarizationService diarizationService, AsrService asrService) {
		Objects.requireNonNull(diarizationService, "diarizationService");
		Objects.requireNonNull(asrService, "asrService");
		log.info("创建 OfflineDiarizationTranscribeService");
		return new OfflineDiarizationTranscribeService(diarizationService, asrService);
	}
}
