package net.dreamlu.mica.voice.exception;

/**
 * sherpa-onnx 底层引擎调用失败异常。
 *
 * <p>封装来自 native 层的错误（如 native 库缺失、ONNX Runtime 初始化失败、
 * 推理越界等）。上层通常应降级到兜底逻辑或直接抛出 5xx。
 *
 * @author dreamlu
 */
public class EngineException extends MicaVoiceException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message 错误信息
	 */
	public EngineException(String message) {
		super(message);
	}

	/**
	 * @param message 错误信息
	 * @param cause   原始异常
	 */
	public EngineException(String message, Throwable cause) {
		super(message, cause);
	}
}
