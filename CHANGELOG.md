# 变更记录

## 发行版本

### [1.0.2] - 2026-09-03

**新功能**

- feat(streaming-tts): mica-voice-example-spring-web 添加流式 TTS WebSocket 示例。

**构建 / 依赖**

- 升级底层 native 依赖 `mica-sherpa-onnx` 至 `1.13.6`

### [1.0.1] - 2026-08-21

**新功能**

- **核心 SDK（`mica-voice-core`）**：新增窗口移动比例配置，并集成到相关服务中，提升音频窗口滑动的灵活性
- **自动装配**：补充各 Service Bean 与配置类的 JavaDoc 注释，增强 IDE 提示与可维护性

**问题修复**

- **关键词唤醒 KWS**：优化关键词识别结果处理逻辑，避免空结果或异常输出（感谢 `@yaochao` 反馈，Gitee Issue `#IK9TAJ`）
- **关键词唤醒 KWS**：修正 KWS 模型目录名为 `sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01`，与 sherpa-onnx 官方命名保持一致

**文档**

- **README**：更新主 README 内容，优化 `mica-voice-examples` 子模块标题描述，删除自动装配对应 starter 类的冗余说明

**构建 / 依赖**

- 升级底层 native 依赖 `mica-sherpa-onnx` 至 `1.13.6`

### [1.0.0] - 2026-08-15

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
