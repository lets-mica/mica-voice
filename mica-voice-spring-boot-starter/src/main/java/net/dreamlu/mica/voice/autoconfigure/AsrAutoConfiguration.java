package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.OfflineAsrService;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
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
 * <p>v1.2+：两个 Bean 的返回类型改为各自的具体实现类（{@link OfflineAsrService} /
 * {@link OnlineAsrService}），不再注册为统一的 {@code AsrService} 接口类型，
 * 避免容器出现同接口多 Bean 时的注入歧义问题。调用方按具体类型注入即可。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(net.dreamlu.mica.voice.core.MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class AsrAutoConfiguration {

	private final MicaVoiceConfig coreProps;
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
	 * 离线 ASR。
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceOfflineAsrService")
	@ConditionalOnProperty(prefix = "mica.voice.asr.offline", name = "enabled", havingValue = "true", matchIfMissing = true)
	public OfflineAsrService micaVoiceOfflineAsrService() {
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
		return new OfflineAsrService(coreProps, asrConfig);
	}

	/**
	 * 在线流式 ASR。
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
			.endpointRule1MinTrailingSilence(cfg.getEndpointRule1MinTrailingSilence())
			.endpointRule2MinTrailingSilence(cfg.getEndpointRule2MinTrailingSilence())
			.endpointRule3MinUtteranceLength(cfg.getEndpointRule3MinUtteranceLength())
			.chunkSize(cfg.getChunkSize())
			.build();
		log.info("mica-voice 装配 OnlineAsrService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return new OnlineAsrService(coreProps, onlineConfig);
	}
}