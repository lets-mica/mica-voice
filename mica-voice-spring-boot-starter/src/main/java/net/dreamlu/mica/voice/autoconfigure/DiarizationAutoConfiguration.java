package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.DiarizationConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.diarization.DiarizationService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 说话人分离自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnClass(MicaVoice.class)
@ConditionalOnBean(name = "micaVoiceCoreProperties")
@ConditionalOnProperty(prefix = "mica.voice.diarization", name = "enabled", havingValue = "true")
@AutoConfigureAfter(MicaVoiceAutoConfiguration.class)
public class DiarizationAutoConfiguration {

	private final net.dreamlu.mica.voice.config.MicaVoiceConfig coreProps;
	private final MicaVoiceProperties props;

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(name = "micaVoiceDiarizationService")
	public DiarizationService micaVoiceDiarizationService() {
		MicaVoiceProperties.Diarization cfg = props.getDiarization();
		DiarizationConfig diarConfig = DiarizationConfig.builder()
			.segmentationModelFileName(cfg.getSegmentationModelFileName())
			.embeddingModelFileName(cfg.getEmbeddingModelFileName())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.numClusters(cfg.getNumClusters())
			.clusterThreshold(cfg.getClusterThreshold())
			.minDurationOff(cfg.getMinDurationOff())
			.minDurationOn(cfg.getMinDurationOn())
			.build();
		log.info("mica-voice 装配 DiarizationService: seg={}, emb={}",
			cfg.getSegmentationModelFileName(), cfg.getEmbeddingModelFileName());
		return MicaVoice.diarization(coreProps, diarConfig);
	}
}
