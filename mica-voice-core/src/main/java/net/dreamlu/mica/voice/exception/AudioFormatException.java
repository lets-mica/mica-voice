package net.dreamlu.mica.voice.exception;

/**
 * 音频格式不支持异常。
 *
 * <p>sherpa-onnx 当前仅原生支持 <b>单声道 16-bit PCM</b> 的 WAV 文件；
 * 多声道、24-bit、mp3/m4a/flac 等格式需先用 ffmpeg 转换。
 *
 * @author dreamlu
 */
public class AudioFormatException extends MicaVoiceException {

	private static final long serialVersionUID = 1L;

	public AudioFormatException(String message) {
		super(message);
	}

	public AudioFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
