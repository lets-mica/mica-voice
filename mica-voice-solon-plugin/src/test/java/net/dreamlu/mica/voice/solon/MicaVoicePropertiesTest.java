/*
 * Copyright (c) 2019-2026, dreamlu.net All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.voice.solon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MicaVoiceProperties} 单元测试：验证默认值与 getter/setter。
 */
class MicaVoicePropertiesTest {

	@Test
	void shouldHaveDefaultValues() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		assertTrue(props.isEnabled());
		assertEquals("models", props.getModelsDir());
		assertEquals("output", props.getOutputDir());
		assertEquals(Integer.valueOf(2), props.getThreads());
		assertFalse(props.isDebug());

		// 嵌套默认
		assertTrue(props.getAsr().getOffline().isEnabled());
		assertEquals("PARAFORMER", props.getAsr().getOffline().getModelType());
		assertEquals("auto", props.getAsr().getOffline().getLanguage());
		assertFalse(props.getAsr().getOnline().isEnabled());
		assertTrue(props.getTts().isEnabled());
		assertEquals("VITS", props.getTts().getModelType());
		assertTrue(props.getSpeaker().isEnabled());
		assertFalse(props.getVad().isEnabled());
		assertFalse(props.getDiarization().isEnabled());
		assertFalse(props.getKws().isEnabled());
		assertFalse(props.getDenoise().isEnabled());
	}

	@Test
	void shouldSetAndGetCoreProperties() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.setModelsDir("/tmp/models");
		props.setOutputDir("/tmp/output");
		props.setThreads(8);
		props.setDebug(true);
		assertEquals("/tmp/models", props.getModelsDir());
		assertEquals("/tmp/output", props.getOutputDir());
		assertEquals(Integer.valueOf(8), props.getThreads());
		assertTrue(props.isDebug());
	}

	@Test
	void shouldSetNestedAsrConfig() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.getAsr().getOffline().setModelDirName("custom-asr");
		props.getAsr().getOffline().setModelType("SENSE_VOICE");
		props.getAsr().getOffline().setLanguage("en");
		assertEquals("custom-asr", props.getAsr().getOffline().getModelDirName());
		assertEquals("SENSE_VOICE", props.getAsr().getOffline().getModelType());
		assertEquals("en", props.getAsr().getOffline().getLanguage());
	}

	@Test
	void shouldToggleAllCapabilities() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.getAsr().getOffline().setEnabled(false);
		props.getAsr().getOnline().setEnabled(true);
		props.getTts().setEnabled(false);
		props.getSpeaker().setEnabled(false);
		props.getVad().setEnabled(true);
		props.getDiarization().setEnabled(true);
		props.getKws().setEnabled(true);
		props.getDenoise().setEnabled(true);

		assertFalse(props.getAsr().getOffline().isEnabled());
		assertTrue(props.getAsr().getOnline().isEnabled());
		assertFalse(props.getTts().isEnabled());
		assertFalse(props.getSpeaker().isEnabled());
		assertTrue(props.getVad().isEnabled());
		assertTrue(props.getDiarization().isEnabled());
		assertTrue(props.getKws().isEnabled());
		assertTrue(props.getDenoise().isEnabled());
	}

	@Test
	void shouldSetVadAndDenoiseConfig() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.getVad().setThreshold(0.6f);
		props.getVad().setMinSilenceDuration(0.3f);
		props.getDenoise().setAttenuationLimitDb(18.0f);
		assertEquals(0.6f, props.getVad().getThreshold());
		assertEquals(0.3f, props.getVad().getMinSilenceDuration());
		assertEquals(18.0f, props.getDenoise().getAttenuationLimitDb());
	}

	@Test
	void shouldSetKwsAndDiarizationConfig() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.getKws().setKeywordsThreshold(0.3f);
		props.getDiarization().setClusterThreshold(0.55f);
		assertEquals(0.3f, props.getKws().getKeywordsThreshold());
		assertEquals(0.55f, props.getDiarization().getClusterThreshold());
	}

	@Test
	void shouldSetTtsConfig() {
		MicaVoiceProperties props = new MicaVoiceProperties();
		props.getTts().setDefaultSpeakerId(3);
		props.getTts().setDefaultSpeed(1.2f);
		assertEquals(3, props.getTts().getDefaultSpeakerId());
		assertEquals(1.2f, props.getTts().getDefaultSpeed());
	}
}