# 变更记录

## 发行版本

### [1.0.0] - 待发布

- **核心 SDK（`mica-voice-core`）**：统一的语音 AI 门面 `MicaVoice`，一行调用即可获取离线 ASR、在线流式 ASR、TTS、声纹识别、VAD、说话人分离、关键词唤醒 KWS、音频降噪等服务（均实现 `AutoCloseable`）
- **离线 ASR**：支持 Paraformer / SenseVoice / Whisper / Moonshine / Zipformer / NeMo CTC 等模型家族，SenseVoice 支持多语言（中 / 英 / 日 / 韩 / 粤）与逆文本规范化
- **在线流式 ASR**：支持 Streaming Paraformer 与 X-ASR（Zipformer Transducer，960ms 分块），可配置端点检测规则（句尾静音自动结束）
- **语音合成 TTS**：基于 VITS，支持多说话人模型与中英混合模型（`vits-melo-tts-zh_en`）
- **声纹识别**：注册 / 验证 / 搜索，支持 3D-Speaker / eres2net 等嵌入模型，相似度阈值可调
- **VAD 语音活动检测、说话人分离、关键词唤醒 KWS、音频降噪**（扩展能力）
- **分离 + 转写组合服务**：自动输出「谁在什么时间说了什么」
- **Spring Boot starter**：`mica.voice.*` 配置开箱即用，自动装配 9 个 Service Bean（`OfflineAsrService` / `OnlineAsrService` / `TtsService` / `SpeakerService` / `VadService` / `DiarizationService` / `KwsService` / `DenoiseService` / `OfflineDiarizationTranscribeService`），应用关闭时统一释放 native 资源
- **Solon 插件（`mica-voice-solon-plugin`）**：与 Spring Boot 完全相同的配置树，一行接入 Solon 应用（JDK 8 ~ 26）
- **集成示例**：纯 Java 控制台（`mica-voice-example-console`）、Spring Boot Web（REST + 浏览器麦克风 → WebSocket 流式 ASR）、Solon Web（`mica-voice-example-solon-web`）
- **模型下载脚本**：支持 Linux / macOS / Windows，多段并行下载与断点续传
- 基于 `mica-sherpa-onnx` 1.13.5（全平台 native fat jar），兼容 JDK 8 / 17 / 21 / 25
