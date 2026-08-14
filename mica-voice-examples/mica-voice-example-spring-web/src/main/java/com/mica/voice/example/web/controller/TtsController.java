package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TTS REST 端点。
 *
 * <pre>
 *   GET  /mica/voice/tts/synthesize?text=...&speakerId=0&speed=1.0  -> audio/wav
 *   GET  /mica/voice/tts/info -> { sampleRate, numSpeakers, defaultSpeakerId, defaultSpeed }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@RestController
@RequestMapping("/mica/voice/tts")
@RequiredArgsConstructor
public class TtsController {

	private final TtsService ttsService;

	/**
	 * 把 float[] [-1,1] 写成单声道 16-bit PCM WAV。
	 */
	static byte[] wrapAsWav(float[] samples, int sampleRate) throws IOException {
		int byteRate = sampleRate * 2;
		int dataLen = samples.length * 2;
		int totalLen = 36 + dataLen;
		ByteBuffer buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
		buf.putInt(totalLen);
		buf.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');
		buf.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
		buf.putInt(16);
		buf.putShort((short) 1);
		buf.putShort((short) 1);
		buf.putInt(sampleRate);
		buf.putInt(byteRate);
		buf.putShort((short) 2);
		buf.putShort((short) 16);
		buf.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
		buf.putInt(dataLen);
		for (float s : samples) {
			float c = Math.max(-1f, Math.min(1f, s));
			buf.putShort((short) (c * 32767f));
		}
		return buf.array();
	}

	@GetMapping(value = "/synthesize", produces = "audio/wav")
	public ResponseEntity<byte[]> synthesize(
		@RequestParam("text") String text,
		@RequestParam(value = "speakerId", defaultValue = "0") int speakerId,
		@RequestParam(value = "speed", defaultValue = "1.0") float speed) throws IOException {
		if (text == null || text.isEmpty()) {
			throw new IllegalArgumentException("text 不能为空");
		}
		TtsAudio audio = ttsService.synthesize(text, speakerId, speed);
		byte[] wav = wrapAsWav(audio.getSamples(), audio.getSampleRate());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("audio/wav"));
		headers.setContentLength(wav.length);
		headers.setContentDispositionFormData("attachment", "tts-" + speakerId + ".wav");
		return new ResponseEntity<>(wav, headers, 200);
	}

	/**
	 * 模型信息（用于调试与 HTML 控制台）。
	 */
	@GetMapping("/info")
	public Map<String, Object> info() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("sampleRate", ttsService.getSampleRate());
		info.put("numSpeakers", ttsService.getNumSpeakers());
		return info;
	}
}
