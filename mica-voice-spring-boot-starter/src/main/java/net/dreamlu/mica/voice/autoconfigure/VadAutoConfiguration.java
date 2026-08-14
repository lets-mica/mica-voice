package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.VadConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.vad.VadService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * VAD 自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.vad", name = "enabled", havingValue = "true")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class VadAutoConfiguration {

	private final MicaVoiceConfig coreProps;
	private final MicaVoiceProperties props;

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceVadService")
	public VadService micaVoiceVadService() {
		MicaVoiceProperties.Vad cfg = props.getVad();
		VadConfig.ModelType type;
		try {
			type = Enum.valueOf(VadConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = VadConfig.ModelType.SILERO;
		}
		VadConfig vadConfig = VadConfig.builder()
			.modelFileName(cfg.getModelFileName())
			.modelType(type)
			.sampleRate(cfg.getSampleRate())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.threshold(cfg.getThreshold())
			.minSilenceDuration(cfg.getMinSilenceDuration())
			.minSpeechDuration(cfg.getMinSpeechDuration())
			.maxSpeechDuration(cfg.getMaxSpeechDuration())
			.windowSize(cfg.getWindowSize())
			.build();
		log.info("mica-voice 装配 VadService: model={}, type={}", cfg.getModelFileName(), cfg.getModelType());
		return MicaVoice.vad(coreProps, vadConfig);
	}
}
