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

	/**
	 * 语音端点检测（VAD）服务。
	 *
	 * @return VAD 服务
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceVadService")
	public VadService micaVoiceVadService(MicaVoiceConfig coreProps, MicaVoiceProperties props) {
		MicaVoiceProperties.Vad cfg = props.getVad();
		VadConfig vadConfig = VadConfig.builder()
			.modelFileName(cfg.getModelFileName())
			.modelType(cfg.getModelType())
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
