package net.dreamlu.mica.voice.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * ASR 自动装配：离线 + 在线两个独立 Bean。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class AsrAutoConfiguration {

	private final net.dreamlu.mica.voice.config.MicaVoiceProperties coreProps;
	private final MicaVoiceProperties props;

	private static <E extends Enum<E>> E parseModelType(String raw, Class<E> type, E fallback) {
		if (raw == null || raw.isEmpty()) {
			return fallback;
		}
		try {
			return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			log.warn("无效的模型类型: {}，回退到 {}", raw, fallback);
			return fallback;
		}
	}

	/**
	 * 离线 ASR
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceOfflineAsrService")
	@ConditionalOnProperty(prefix = "mica.voice.asr.offline", name = "enabled", havingValue = "true", matchIfMissing = true)
	public AsrService micaVoiceOfflineAsrService() {
		MicaVoiceProperties.Asr.Offline cfg = props.getAsr().getOffline();
		AsrConfig asrConfig = AsrConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(parseModelType(cfg.getModelType(), AsrConfig.ModelType.class, AsrConfig.ModelType.PARAFORMER))
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.language(cfg.getLanguage())
			.inverseTextNormalization(cfg.isInverseTextNormalization())
			.build();
		log.info("mica-voice 装配 OfflineAsrService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.asr(coreProps, asrConfig);
	}

	/**
	 * 在线流式 ASR
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceOnlineAsrService")
	@ConditionalOnProperty(prefix = "mica.voice.asr.online", name = "enabled", havingValue = "true")
	public OnlineAsrService micaVoiceOnlineAsrService() {
		MicaVoiceProperties.Asr.Online cfg = props.getAsr().getOnline();
		OnlineAsrConfig onlineConfig = OnlineAsrConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(parseModelType(cfg.getModelType(), OnlineAsrConfig.ModelType.class, OnlineAsrConfig.ModelType.PARAFORMER))
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.enableEndpoint(cfg.isEnableEndpoint())
			.chunkSize(cfg.getChunkSize())
			.build();
		log.info("mica-voice 装配 OnlineAsrService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.onlineAsrTyped(coreProps, onlineConfig);
	}
}
