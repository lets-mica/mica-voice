package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.DenoiseConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.denoise.DenoiseService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * 音频降噪自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.denoise", name = "enabled", havingValue = "true")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class DenoiseAutoConfiguration {

	private final MicaVoiceConfig coreProps;
	private final MicaVoiceProperties props;

	/**
	 * 音频降噪服务。
	 *
	 * @return 音频降噪服务
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceDenoiseService")
	public DenoiseService micaVoiceDenoiseService() {
		MicaVoiceProperties.Denoise cfg = props.getDenoise();
		DenoiseConfig.ModelType type;
		try {
			type = Enum.valueOf(DenoiseConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = DenoiseConfig.ModelType.GTCRN;
		}
		DenoiseConfig denoiseConfig = DenoiseConfig.builder()
			.modelFileName(cfg.getModelFileName())
			.modelType(type)
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.attenuationLimitDb(cfg.getAttenuationLimitDb())
			.build();
		log.info("mica-voice 装配 DenoiseService: model={}, type={}", cfg.getModelFileName(), cfg.getModelType());
		return MicaVoice.denoise(coreProps, denoiseConfig);
	}
}
