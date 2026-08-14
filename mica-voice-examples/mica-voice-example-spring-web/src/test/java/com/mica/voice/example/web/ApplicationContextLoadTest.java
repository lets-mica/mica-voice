package com.mica.voice.example.web;

import net.dreamlu.mica.voice.autoconfigure.MicaVoiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 ApplicationContext 能正常启动 + starter 装配链路通畅。
 *
 * <p>不依赖任何模型文件（默认 models-dir 找不到也不会让启动失败，
 * 因为 starter 的能力装配都带了 {@code @ConditionalOnBean} 检查），
 * 但 starter 本身装配的 {@code micaVoiceCoreProperties} Bean 应该存在。
 *
 * @author dreamlu
 */
@SpringBootTest
@TestPropertySource(properties = {
        // 跑测试时把模型目录指向一个临时目录，避免污染真实目录
        "mica.voice.models-dir=${java.io.tmpdir}/mica-voice-test-models",
        "mica.voice.output-dir=${java.io.tmpdir}/mica-voice-test-output",
        // 关掉模型加载可能失败的能力，确保 context 能起来
        "mica.voice.asr.offline.enabled=false",
        "mica.voice.tts.enabled=false",
        "mica.voice.speaker.enabled=false"
})
class ApplicationContextLoadTest {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private MicaVoiceProperties starterProps;

    @Test
    void contextLoads() {
        assertNotNull(ctx, "ApplicationContext 应正常启动");
    }

    @Test
    void starterBeanWired() {
        assertNotNull(starterProps, "starter MicaVoiceProperties 应注入");
        // starter 暴露的 core properties bean 应存在
        Object coreProps = ctx.getBean("micaVoiceCoreProperties");
        assertNotNull(coreProps, "micaVoiceCoreProperties Bean 应存在");
        // 类型应是 core 的 MicaVoiceProperties
        assertTrue(coreProps instanceof net.dreamlu.mica.voice.config.MicaVoiceProperties);
    }
}
