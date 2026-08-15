/**
 * mica-voice Spring Boot 自动装配与 starter。
 *
 * <p>使用方式：在 {@code application.yml} 中配置 {@code mica.voice.*}，
 * 引入 starter 依赖即可自动注入 {@code OfflineAsrService / OnlineAsrService /
 * TtsService / SpeakerService / VadService / DiarizationService / KwsService /
 * DenoiseService / OfflineDiarizationTranscribeService} 等 Bean。
 *
 * <p>v1.2+：ASR 不再以 {@code AsrService} 接口暴露（避免同接口多 Bean 的注入歧义），
 * 改为 {@link net.dreamlu.mica.voice.asr.OfflineAsrService} /
 * {@link net.dreamlu.mica.voice.asr.OnlineAsrService} 两个具体类型。
 *
 * <p>Spring Boot 3.x 基于 Jakarta EE。
 *
 * @author dreamlu
 */
package net.dreamlu.mica.voice.autoconfigure;