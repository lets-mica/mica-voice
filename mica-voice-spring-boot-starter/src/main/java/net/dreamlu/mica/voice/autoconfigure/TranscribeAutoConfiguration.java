package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.OfflineAsrService;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.diarization.DiarizationService;
import net.dreamlu.mica.voice.transcribe.OfflineDiarizationTranscribeService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "说话人分离 + 转写"联合服务自动装配。
 *
 * <p>扩展能力。当 {@code mica.voice.asr.offline.enabled=true} 且
 * {@code mica.voice.diarization.enabled=true} 时生效。
 *
 * <p>依赖的 ASR Bean 类型从 {@code AsrService} 接口收紧为
 * {@link OfflineAsrService} 具体类型，避免离线/在线两个 ASR 都实现同一接口时
 * 容器内 {@code ObjectProvider<AsrService>} 的注入歧义。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@AutoConfigureAfter({AsrAutoConfiguration.class, DiarizationAutoConfiguration.class})
public class TranscribeAutoConfiguration {

	private final ObjectProvider<OfflineAsrService> offlineAsrProvider;
	private final ObjectProvider<DiarizationService> diarizationProvider;

	@Bean(destroyMethod = "close")
	@ConditionalOnBean(name = {"micaVoiceOfflineAsrService", "micaVoiceDiarizationService"})
	public OfflineDiarizationTranscribeService micaVoiceDiarizationTranscribeService() {
		OfflineAsrService asr = offlineAsrProvider.getIfAvailable();
		DiarizationService diarization = diarizationProvider.getIfAvailable();
		if (asr == null || diarization == null) {
			// 不抛错：Bean 仅在两个能力都启用时才存在；启动顺序由 @ConditionalOnBean 保证
			return null;
		}
		log.info("mica-voice 装配 OfflineDiarizationTranscribeService");
		return MicaVoice.transcribe(diarization, asr);
	}
}