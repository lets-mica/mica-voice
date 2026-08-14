package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.SpeakerConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 声纹识别自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.speaker", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class SpeakerAutoConfiguration {

	private final net.dreamlu.mica.voice.config.MicaVoiceConfig coreProps;
	private final MicaVoiceProperties props;

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceSpeakerService")
	public SpeakerService micaVoiceSpeakerService() {
		MicaVoiceProperties.Speaker cfg = props.getSpeaker();
		SpeakerConfig speakerConfig = SpeakerConfig.builder()
			.modelCandidates(cfg.getModelCandidates() == null
				? SpeakerConfig.DEFAULT_MODEL_CANDIDATES
				: cfg.getModelCandidates())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.threshold(cfg.getThreshold())
			.embeddingTimeoutMs(cfg.getEmbeddingTimeoutMs())
			.build();
		log.info("mica-voice 装配 SpeakerService: threshold={}, candidates={}",
			cfg.getThreshold(), java.util.Arrays.toString(cfg.getModelCandidates()));
		return MicaVoice.speaker(coreProps, speakerConfig);
	}
}
