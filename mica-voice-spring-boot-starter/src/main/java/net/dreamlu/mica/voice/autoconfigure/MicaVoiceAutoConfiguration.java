package net.dreamlu.mica.voice.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * mica-voice Spring Boot 顶层装配入口。
 *
 * <p>负责：
 * <ul>
 *     <li>把 starter 的 {@link MicaVoiceProperties} 转为 core 的
 *         {@link net.dreamlu.mica.voice.config.MicaVoiceConfig}
 *         作为可注入 Bean（其它 Bean 注入运行时属性都从这里取）</li>
 *     <li>各能力（ASR / TTS / Speaker / Web）通过
 *         {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *         自动发现与装配</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableConfigurationProperties(MicaVoiceProperties.class)
@ConditionalOnProperty(prefix = "mica.voice", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MicaVoiceAutoConfiguration {

	private final MicaVoiceProperties props;

	/**
	 * 把 starter 的扁平配置转换成 core 用的运行时
	 * {@link net.dreamlu.mica.voice.config.MicaVoiceConfig}。
	 * 该 Bean 是 core 层各 Service 构造时的统一入口（命名 micaVoiceCoreProperties）。
	 *
	 * @return core 层统一的 MicaVoiceConfig
	 */
	@Bean(name = "micaVoiceCoreProperties")
	public MicaVoiceConfig coreProperties() {
		MicaVoiceConfig p =
			new MicaVoiceConfig();
		p.setModelsDir(new File(props.getModelsDir()));
		p.setOutputDir(new File(props.getOutputDir()));
		if (props.getThreads() != null) {
			p.setThreads(props.getThreads());
		}
		p.setDebug(props.isDebug());
		log.info("mica-voice 运行时属性初始化: modelsDir={}, outputDir={}, threads={}, debug={}",
			p.getModelsDir(), p.getOutputDir(), p.getThreads(), p.isDebug());
		return p;
	}
}
