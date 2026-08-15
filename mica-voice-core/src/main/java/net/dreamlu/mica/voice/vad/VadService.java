package net.dreamlu.mica.voice.vad;

import net.dreamlu.mica.voice.audio.AudioData;

import java.util.List;

/**
 * VAD（语音活动检测）服务接口。
 *
 * <p>典型用法：把整段音频喂给 {@link #detect(AudioData)}，返回所有检测到的语音片段。
 * 资源用尽后建议 {@link #close()}（也支持 try-with-resources）。
 *
 * @author dreamlu
 */
public interface VadService extends AutoCloseable {

	/**
	 * 从完整音频中检测所有语音片段。
	 *
	 * @param audio 完整音频
	 * @return 语音片段列表（按时间顺序）
	 */
	List<VadSegment> detect(AudioData audio);

/**
	 * 流式检测：每帧（chunk）调用一次 {@link #acceptWaveform(float[])}，用 {@link #poll()} 取出已完成的片段。
	 *
	 * @param samples 当前帧的样本（单声道 float[]）
	 */
	void acceptWaveform(float[] samples);

	/**
	 * 取出所有已就绪的语音片段（消费式，从内部队列 pop）。
	 *
	 * @return 当前可消费的语音片段列表
	 */
	List<VadSegment> poll();

	/**
	 * 是否检测到语音（任一未消费的片段仍在语音段内）。
	 *
	 * @return true 表示当前仍有未消费的语音活动
	 */
	boolean isSpeechDetected();

	/**
	 * 流结束时调用，确保剩余的语音片段被切出。
	 */
	void flush();

	/**
	 * 重置内部状态。
	 */
	void reset();

	@Override
	void close();
}
