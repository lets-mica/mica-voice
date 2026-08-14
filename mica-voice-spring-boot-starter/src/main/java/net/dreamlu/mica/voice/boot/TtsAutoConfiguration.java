package net.dreamlu.mica.voice.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.tts.TtsService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * TTS 自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.tts", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class TtsAutoConfiguration {

	private final net.dreamlu.mica.voice.config.MicaVoiceProperties coreProps;
	private final MicaVoiceProperties props;

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceTtsService")
	public TtsService micaVoiceTtsService() {
		MicaVoiceProperties.Tts cfg = props.getTts();
		TtsConfig.ModelType type;
		try {
			type = Enum.valueOf(TtsConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = TtsConfig.ModelType.VITS;
		}
		TtsConfig ttsConfig = TtsConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(type)
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.defaultSpeakerId(cfg.getDefaultSpeakerId())
			.defaultSpeed(cfg.getDefaultSpeed())
			.callbackSampleStep(cfg.getCallbackSampleStep())
			.build();
		log.info("mica-voice 装配 TtsService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.tts(coreProps, ttsConfig);
	}
}
