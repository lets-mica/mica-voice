package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.denoise.DenoiseService;
import net.dreamlu.mica.voice.denoise.DenoisedAudio;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Denoise REST 端点。
 *
 * <pre>
 *   POST /mica/voice/denoise/run  file=@input.wav  -> audio/wav（降噪后的 wav）
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@RestController
@RequestMapping("/mica/voice/denoise")
@RequiredArgsConstructor
public class DenoiseController {

	private final ObjectProvider<DenoiseService> denoiseProvider;

	private static byte[] wrapAsWav(float[] samples, int sampleRate) throws IOException {
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

	private static File toTmp(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-denoise-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> run(@RequestParam("file") MultipartFile file) throws IOException {
		DenoiseService denoise = denoiseProvider.getIfAvailable();
		if (denoise == null) {
			throw new IllegalStateException("Denoise 服务未启用，请设置 mica.voice.denoise.enabled=true");
		}
		AudioData audio = AudioReaders.read(toTmp(file));
		DenoisedAudio out = denoise.denoise(audio);
		byte[] wav = wrapAsWav(out.getSamples(), out.getSampleRate());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("audio/wav"));
		headers.setContentLength(wav.length);
		headers.setContentDispositionFormData("attachment", "denoised.wav");
		return new ResponseEntity<>(wav, headers, 200);
	}

	@PostMapping("/info")
	public Map<String, Object> info(@RequestParam("file") MultipartFile file) throws Exception {
		DenoiseService denoise = denoiseProvider.getIfAvailable();
		if (denoise == null) {
			throw new IllegalStateException("Denoise 服务未启用，请设置 mica.voice.denoise.enabled=true");
		}
		AudioData audio = AudioReaders.read(toTmp(file));
		DenoisedAudio out = denoise.denoise(audio);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("inputSamples", audio.getSamples().length);
		resp.put("outputSamples", out.getSamples().length);
		resp.put("sampleRate", out.getSampleRate());
		resp.put("durationSec", out.durationSeconds());
		resp.put("costMs", out.getCostMs());
		return resp;
	}
}
