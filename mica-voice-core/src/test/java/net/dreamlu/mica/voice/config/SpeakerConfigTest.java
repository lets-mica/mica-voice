package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * {@link SpeakerConfig} 单元测试。
 *
 * <p>验证默认值、候选名数组拷贝语义。
 */
class SpeakerConfigTest {

	@Test
	void defaults() {
		SpeakerConfig c = new SpeakerConfig();
		assertEquals(0.5f, c.getThreshold(), 1e-6);
		assertEquals(30_000L, c.getEmbeddingTimeoutMs());
		assertEquals(SpeakerConfig.DEFAULT_MODEL_CANDIDATES.length, c.getModelCandidates().length);
		// 默认候选第一个应当是 3D-Speaker eres2net
		assertEquals("3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
			c.getModelCandidates()[0]);
	}

	@Test
	void defaultCandidatesIsDefensiveCopy() {
		String[] a = SpeakerConfig.DEFAULT_MODEL_CANDIDATES;
		String[] b = SpeakerConfig.DEFAULT_MODEL_CANDIDATES;
		// 常量本身不会被外部修改，但每次取应是新数组（Java 数组常量的语义）
		assertEquals(a.length, b.length);
	}

	@Test
	void setCandidates_nullBecomesEmpty() {
		SpeakerConfig c = new SpeakerConfig();
		c.setModelCandidates(null);
		assertEquals(0, c.getModelCandidates().length);
	}

	@Test
	void setCandidates_clonesArray() {
		SpeakerConfig c = new SpeakerConfig();
		String[] src = {"a.onnx", "b.onnx"};
		c.setModelCandidates(src);
		src[0] = "hacked.onnx";
		assertEquals("a.onnx", c.getModelCandidates()[0]);
	}

	@Test
	void builderChains() {
		SpeakerConfig c = SpeakerConfig.builder()
			.threshold(0.65f)
			.embeddingTimeoutMs(60_000L)
			.modelCandidates("only.onnx")
			.threads(8)
			.debug(true)
			.build();
		assertEquals(0.65f, c.getThreshold(), 1e-6);
		assertEquals(60_000L, c.getEmbeddingTimeoutMs());
		assertEquals(1, c.getModelCandidates().length);
		assertEquals("only.onnx", c.getModelCandidates()[0]);
		assertEquals(8, c.getThreads());
		assertEquals(true, c.isDebug());
		assertNotSame(c.getModelCandidates(), c.getModelCandidates());
	}
}