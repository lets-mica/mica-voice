package net.dreamlu.mica.voice.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MicaVoiceAutoConfiguration} 单元测试。
 *
 * <p>本测试只覆盖 starter 的装配条件 + 配置树绑定，避免触发任何 native 装配。
 * mica.voice.enabled=false → 装配类不激活；=true（默认）→ micaVoiceCoreProperties Bean 创建。
 */
class MicaVoiceAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfig.class)
		.withConfiguration(AutoConfigurations.of(MicaVoiceAutoConfiguration.class));

	@Test
	void disabledGlobally_yieldsNoCorePropertiesBean() {
		runner.withPropertyValues(
			"mica.voice.enabled=false"
		).run(ctx -> {
			assertThat(ctx).hasNotFailed();
			assertThat(ctx).doesNotHaveBean("micaVoiceCoreProperties");
			// starter 的 MicaVoiceProperties 仍由 @EnableConfigurationProperties 注册
			assertThat(ctx).hasSingleBean(MicaVoiceProperties.class);
		});
	}

	@Test
	void enabledByDefault_createsCorePropertiesBean() {
		runner.run(ctx -> {
			assertThat(ctx).hasNotFailed();
			assertThat(ctx).hasBean("micaVoiceCoreProperties");
			assertThat(ctx).hasSingleBean(MicaVoiceProperties.class);

			// starter 的 MicaVoiceProperties（yml 输入）
			MicaVoiceProperties p = ctx.getBean(MicaVoiceProperties.class);
			assertThat(p.getModelsDir()).isEqualTo("models");
			assertThat(p.getOutputDir()).isEqualTo("output");
			assertThat(p.getThreads()).isEqualTo(2);
			assertThat(p.isDebug()).isFalse();

			// 转换后注入给 core 的 MicaVoiceProperties
			net.dreamlu.mica.voice.config.MicaVoiceProperties core =
				ctx.getBean("micaVoiceCoreProperties", net.dreamlu.mica.voice.config.MicaVoiceProperties.class);
			assertThat(core.getModelsDir()).isEqualTo(new java.io.File("models"));
			assertThat(core.getOutputDir()).isEqualTo(new java.io.File("output"));
			assertThat(core.getThreads()).isEqualTo(2);
			assertThat(core.isDebug()).isFalse();
		});
	}

	@Test
	void bindsNestedConfigTree() {
		runner.withPropertyValues(
			"mica.voice.enabled=true",
			"mica.voice.models-dir=/tmp/test-models",
			"mica.voice.output-dir=/tmp/test-output",
			"mica.voice.threads=8",
			"mica.voice.debug=true",
			"mica.voice.asr.offline.model-dir-name=custom-asr",
			"mica.voice.asr.offline.model-type=SENSE_VOICE",
			"mica.voice.asr.offline.language=en",
			"mica.voice.tts.model-dir-name=custom-tts",
			"mica.voice.tts.default-speaker-id=3",
			"mica.voice.tts.default-speed=1.2",
			"mica.voice.speaker.threshold=0.7",
			"mica.voice.vad.threshold=0.6",
			"mica.voice.diarization.cluster-threshold=0.55",
			"mica.voice.kws.keywords-threshold=0.3",
			"mica.voice.denoise.attenuation-limit-db=18.0"
		).run(ctx -> {
			assertThat(ctx).hasNotFailed();

			MicaVoiceProperties p = ctx.getBean(MicaVoiceProperties.class);
			assertThat(p.getModelsDir()).isEqualTo("/tmp/test-models");
			assertThat(p.getOutputDir()).isEqualTo("/tmp/test-output");
			assertThat(p.getThreads()).isEqualTo(8);
			assertThat(p.isDebug()).isTrue();

			assertThat(p.getAsr().getOffline().getModelDirName()).isEqualTo("custom-asr");
			assertThat(p.getAsr().getOffline().getModelType()).isEqualTo("SENSE_VOICE");
			assertThat(p.getAsr().getOffline().getLanguage()).isEqualTo("en");
			assertThat(p.getTts().getModelDirName()).isEqualTo("custom-tts");
			assertThat(p.getTts().getDefaultSpeakerId()).isEqualTo(3);
			assertThat(p.getTts().getDefaultSpeed()).isEqualTo(1.2f);
			assertThat(p.getSpeaker().getThreshold()).isEqualTo(0.7f);
			assertThat(p.getVad().getThreshold()).isEqualTo(0.6f);
			assertThat(p.getDiarization().getClusterThreshold()).isEqualTo(0.55f);
			assertThat(p.getKws().getKeywordsThreshold()).isEqualTo(0.3f);
			assertThat(p.getDenoise().getAttenuationLimitDb()).isEqualTo(18.0f);

			net.dreamlu.mica.voice.config.MicaVoiceProperties core =
				ctx.getBean("micaVoiceCoreProperties", net.dreamlu.mica.voice.config.MicaVoiceProperties.class);
			assertThat(core.getModelsDir()).isEqualTo(new java.io.File("/tmp/test-models"));
			assertThat(core.getOutputDir()).isEqualTo(new java.io.File("/tmp/test-output"));
			assertThat(core.getThreads()).isEqualTo(8);
			assertThat(core.isDebug()).isTrue();
		});
	}

	@Test
	void threadsInStarterOverridesCore() {
		runner.withPropertyValues(
			"mica.voice.enabled=true",
			"mica.voice.threads=16"
		).run(ctx -> {
			net.dreamlu.mica.voice.config.MicaVoiceProperties core =
				ctx.getBean("micaVoiceCoreProperties", net.dreamlu.mica.voice.config.MicaVoiceProperties.class);
			assertThat(core.getThreads()).isEqualTo(16);
		});
	}

	@Test
	void threadsNullInStarterFallsBackToCoreDefault() {
		runner.withPropertyValues(
			"mica.voice.enabled=true"
			// 不设置 threads
		).run(ctx -> {
			net.dreamlu.mica.voice.config.MicaVoiceProperties core =
				ctx.getBean("micaVoiceCoreProperties", net.dreamlu.mica.voice.config.MicaVoiceProperties.class);
			// core 默认 2
			assertThat(core.getThreads()).isEqualTo(2);
		});
	}

	/**
	 * 测试配置：开启 {@code @EnableConfigurationProperties}，让 starter 的
	 * {@link MicaVoiceProperties} 注册成可绑定的 Bean。
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MicaVoiceProperties.class)
	static class TestConfig {
	}
}