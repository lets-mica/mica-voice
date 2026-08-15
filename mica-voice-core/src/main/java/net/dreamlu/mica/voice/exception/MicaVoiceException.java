package net.dreamlu.mica.voice.exception;

/**
 * mica-voice 顶层异常。
 *
 * <p>所有 mica-voice 抛出的受检/非受检异常的根类型，便于上层做统一捕获与降级。
 *
 * @author dreamlu
 */
public class MicaVoiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message 错误信息
	 */
	public MicaVoiceException(String message) {
		super(message);
	}

	/**
	 * @param message 错误信息
	 * @param cause   原始异常
	 */
	public MicaVoiceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause 原始异常
	 */
	public MicaVoiceException(Throwable cause) {
		super(cause);
	}
}
