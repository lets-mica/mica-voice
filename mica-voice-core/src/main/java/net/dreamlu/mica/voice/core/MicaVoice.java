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
import net.dreamlu.mica.voice.transcribe.OfflineDiarizationTranscribeService;
import net.dreamlu.mica.voice.tts.OfflineTtsService;
import net.dreamlu.mica.voice.tts.TtsService;
import net.dreamlu.mica.voice.vad.SileroVadService;
import net.dreamlu.mica.voice.vad.VadService;

import java.util.Objects;

/**
 * mica-voice 统一门面（静态工厂风格）。
 *
 * <p>位于 {@code net.dreamlu.mica.voice.core} 包，作为 mica-voice 核心能力的"一级"入口。
 *
 * <p>基础能力：asr / onlineAsr / tts / speaker
 * <br>扩展能力：vad / diarization / kws / denoise
 *
 * <p>门面返回的服务对象都实现了 {@link AutoCloseable}，强烈建议用 try-with-resources。
 *
 * @author dreamlu
 */
@Slf4j
public final class MicaVoice {

	private MicaVoice() {
	}

	// =========================== 基础能力 ===========================

	/**
	 * 离线 ASR。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 离线 ASR 配置
	 * @return 同步 ASR 服务（{@link AutoCloseable}）
	 */
	public static AsrService asr(MicaVoiceConfig props, AsrConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineAsrService: modelDir={}", config.getModelDirName());
		return new OfflineAsrService(props, config);
	}

	/**
	 * 在线流式 ASR（{@link AsrService} 形态）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 在线 ASR 配置
	 * @return 流式 ASR 服务
	 */
	public static AsrService onlineAsr(MicaVoiceConfig props, OnlineAsrConfig config) {
		return onlineAsrTyped(props, config);
	}

	/**
	 * 在线流式 ASR（强类型，便于用 createStream 等扩展方法）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 在线 ASR 配置
	 * @return {@link OnlineAsrService} 强类型实例
	 */
	public static OnlineAsrService onlineAsrTyped(MicaVoiceConfig props, OnlineAsrConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OnlineAsrService: modelDir={}", config.getModelDirName());
		return new OnlineAsrService(props, config);
	}

	/**
	 * TTS（语音合成）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config TTS 配置
	 * @return TTS 服务
	 */
	public static TtsService tts(MicaVoiceConfig props, TtsConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineTtsService: modelDir={}", config.getModelDirName());
		return new OfflineTtsService(props, config);
	}

	/**
	 * 声纹识别（注册 / 验证 / 1:N 搜索）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 声纹配置
	 * @return 声纹服务
	 */
	public static SpeakerService speaker(MicaVoiceConfig props, SpeakerConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 SpeakerEmbeddingService");
		return new SpeakerEmbeddingService(props, config);
	}

	// =========================== 扩展能力 ===========================

	/**
	 * VAD（语音活动检测）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config VAD 配置
	 * @return VAD 服务
	 */
	public static VadService vad(MicaVoiceConfig props, VadConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 SileroVadService: model={}", config.getModelFileName());
		return new SileroVadService(props, config);
	}

	/**
	 * 说话人分离。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 说话人分离配置
	 * @return 说话人分离服务
	 */
	public static DiarizationService diarization(MicaVoiceConfig props, DiarizationConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 PyannoteDiarizationService");
		return new PyannoteDiarizationService(props, config);
	}

	/**
	 * 关键词唤醒（KWS）。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config KWS 配置
	 * @return KWS 服务
	 */
	public static KwsService kws(MicaVoiceConfig props, KwsConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 KeywordSpotterService: modelDir={}", config.getModelDirName());
		return new KeywordSpotterService(props, config);
	}

	/**
	 * 音频降噪。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config 降噪配置
	 * @return 降噪服务
	 */
	public static DenoiseService denoise(MicaVoiceConfig props, DenoiseConfig config) {
		Objects.requireNonNull(props, "props");
		Objects.requireNonNull(config, "config");
		log.info("创建 OfflineSpeechDenoiserService: model={}", config.getModelFileName());
		return new OfflineSpeechDenoiserService(props, config);
	}

	/**
	 * "说话人分离 + 转写"联合服务。
	 *
	 * <p>传入 DiarizationService + AsrService（通常都使用 mica-voice 装配的离线版本，
	 * 即 {@link OfflineAsrService}），返回组合后的转写结果：每个说话人在某段时间的文本。
	 *
	 * @param diarizationService 说话人分离服务
	 * @param asrService         ASR 服务（离线版本）
	 * @return 联合转写服务
	 */
	public static OfflineDiarizationTranscribeService transcribe(
		DiarizationService diarizationService, AsrService asrService) {
		Objects.requireNonNull(diarizationService, "diarizationService");
		Objects.requireNonNull(asrService, "asrService");
		log.info("创建 OfflineDiarizationTranscribeService");
		return new OfflineDiarizationTranscribeService(diarizationService, asrService);
	}
}
