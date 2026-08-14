/**
 * mica-voice ASR（语音识别）能力。
 *
 * <p>v1.0 提供：
 * <ul>
 *     <li>{@link net.dreamlu.mica.voice.asr.AsrService}：统一接口（同步）</li>
 *     <li>{@link net.dreamlu.mica.voice.asr.OfflineAsrService}：sherpa-onnx {@code OfflineRecognizer} 适配</li>
 *     <li>{@link net.dreamlu.mica.voice.asr.OnlineAsrService}：流式识别（{@code OnlineRecognizer}）</li>
 * </ul>
 *
 * @author dreamlu
 */
package net.dreamlu.mica.voice.asr;
