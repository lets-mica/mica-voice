package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.KwsConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.kws.KwsService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 关键词唤醒（KWS）自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.kws", name = "enabled", havingValue = "true")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class KwsAutoConfiguration {

	private final net.dreamlu.mica.voice.config.MicaVoiceConfig coreProps;
	private final MicaVoiceProperties props;

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceKwsService")
	public KwsService micaVoiceKwsService() {
		MicaVoiceProperties.Kws cfg = props.getKws();
		KwsConfig kwsConfig = KwsConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.sampleRate(cfg.getSampleRate())
			.featureDim(cfg.getFeatureDim())
			.keywordsScore(cfg.getKeywordsScore())
			.keywordsThreshold(cfg.getKeywordsThreshold())
			.maxActivePaths(cfg.getMaxActivePaths())
			.keywordsFile(cfg.getKeywordsFile())
			.build();
		log.info("mica-voice 装配 KwsService: modelDir={}", cfg.getModelDirName());
		return MicaVoice.kws(coreProps, kwsConfig);
	}
}
