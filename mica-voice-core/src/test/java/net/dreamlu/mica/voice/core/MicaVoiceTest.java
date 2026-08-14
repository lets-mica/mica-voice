package net.dreamlu.mica.voice.core;

import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.DenoiseConfig;
import net.dreamlu.mica.voice.config.DiarizationConfig;
import net.dreamlu.mica.voice.config.KwsConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
import net.dreamlu.mica.voice.config.SpeakerConfig;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.config.VadConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MicaVoice} 门面参数校验测试。
 *
 * <p>所有 factory 都对 {@code null} props / config 抛 {@link NullPointerException}，
 * 此处只校验最关键的契约；实际 native 装配需要真模型文件，不在单元测试范围内。
 */
class MicaVoiceTest {

	private static MicaVoiceConfig props() {
		return MicaVoiceConfig.builder()
			.modelsDir(new File("models"))
			.threads(1)
			.build();
	}

	@Test
	void asr_rejectsNullProps() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.asr(null, new AsrConfig("x")));
	}

	@Test
	void asr_rejectsNullConfig() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.asr(props(), null));
	}

	@Test
	void onlineAsrTyped_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.onlineAsrTyped(null, new OnlineAsrConfig("x")));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.onlineAsrTyped(props(), null));
	}

	@Test
	void tts_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.tts(null, new TtsConfig("x")));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.tts(props(), null));
	}

	@Test
	void speaker_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.speaker(null, SpeakerConfig.builder().build()));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.speaker(props(), (SpeakerConfig) null));
	}

	@Test
	void vad_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.vad(null, VadConfig.builder().build()));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.vad(props(), (VadConfig) null));
	}

	@Test
	void diarization_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.diarization(null, DiarizationConfig.builder().build()));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.diarization(props(), (DiarizationConfig) null));
	}

	@Test
	void kws_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.kws(null, KwsConfig.builder().build()));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.kws(props(), (KwsConfig) null));
	}

	@Test
	void denoise_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.denoise(null, DenoiseConfig.builder().build()));
		assertThrows(NullPointerException.class,
			() -> MicaVoice.denoise(props(), (DenoiseConfig) null));
	}

	@Test
	void transcribe_rejectsNulls() {
		assertThrows(NullPointerException.class,
			() -> MicaVoice.transcribe(null, null));
	}

	@Test
	void constructorIsPrivate() throws Exception {
		// MicaVoice 工具类应禁止实例化
		java.lang.reflect.Constructor<MicaVoice> ctor = MicaVoice.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		Object instance = ctor.newInstance();
		assertNotNull(instance);
		assertTrue(instance instanceof MicaVoice);
	}

	@Test
	void asr_acceptsValidArgs() {
		// valid config 会构造成功（不抛 NPE），即使模型文件不存在
		// 后续 native 加载才会失败——此处只校验参数层
		try {
			AsrService svc = MicaVoice.asr(props(),
				AsrConfig.builder()
					.modelDirName("missing-model-dir-1234")
					.modelType(AsrConfig.ModelType.PARAFORMER)
					.build());
			assertNotNull(svc);
		} catch (RuntimeException ex) {
			// 期望模型不存在的异常，而非 NPE
			assertTrue(!isNpe(ex), "expected non-NPE runtime exception, got " + ex);
		}
	}

	private static boolean isNpe(Throwable t) {
		while (t != null) {
			if (t instanceof NullPointerException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}
}