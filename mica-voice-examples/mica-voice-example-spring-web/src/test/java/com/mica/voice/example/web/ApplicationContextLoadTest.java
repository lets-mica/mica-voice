package com.mica.voice.example.web;

import net.dreamlu.mica.voice.autoconfigure.MicaVoiceAutoConfiguration;
import net.dreamlu.mica.voice.autoconfigure.MicaVoiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 mica-voice-spring-boot-starter 的装配链路通畅。
 *
 * <p>只装配 starter 的顶层 {@link MicaVoiceAutoConfiguration}（其中包含
 * {@code micaVoiceCoreProperties} Bean），不装配示例项目的业务 controller，
 * 也不触发任何模型加载。该测试是烟雾测试，与 starter 模块的
 * {@code MicaVoiceAutoConfigurationTest} 互补。
 *
 * @author dreamlu
 */
class ApplicationContextLoadTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MicaVoiceAutoConfiguration.class))
		.withUserConfiguration(TestConfig.class);

	@Test
	void starterBeanWired() {
		runner.run(ctx -> {
			assertThat(ctx).hasNotFailed();
			assertThat(ctx).hasBean("micaVoiceCoreProperties");
			assertThat(ctx).hasSingleBean(MicaVoiceProperties.class);

			Object coreProps = ctx.getBean("micaVoiceCoreProperties");
			assertThat(coreProps).isInstanceOf(net.dreamlu.mica.voice.config.MicaVoiceProperties.class);
		});
	}

	/**
	 * 让 starter 的 {@link MicaVoiceProperties} 注册成可绑定 Bean。
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MicaVoiceProperties.class)
	static class TestConfig {
	}
}
