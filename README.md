# mica-voice

![Java](https://img.shields.io/badge/Java-8%2B-orange)

> Java 生态的声音 AI 全家桶：ASR / TTS / 声纹 / VAD / 说话人分离 / 降噪 / KWS。
>
> 基于 [`mica-sherpa-onnx`](https://github.com/dreamlu/mica-sherpa-onnx)（已发布 Maven Central 的 sherpa-onnx 全平台 fat jar）。

## 模块一览

| 模块 | 作用 |
| ---- | ---- |
| `mica-voice-core` | **核心 SDK**（纯 Java）。统一 ASR / TTS / 声纹 / VAD 等 API，强依赖 `mica-sherpa-onnx` |
| `mica-voice-spring-boot-starter` | **Spring Boot 自动装配**。`mica.voice.*` 配置即开箱即用，注入 `AsrService / TtsService / SpeakerService` Bean |
| `mica-voice-examples` | **集成示例聚合**，展示 mica-voice 在真实业务场景下怎么用 |

### `mica-voice-examples` 子模块（按需展开）

| 子模块 | 作用 |
| ------ | ---- |
| `mica-voice-example-console` | 纯 Java main 示例：直接用 `mica-voice-core` 门面 + `models/` 模型，命令行测 ASR / SenseVoice / X-ASR / TTS / 声纹 |
| `mica-voice-example-spring-web` | Spring Boot Web 示例：REST 上传 wav 做 ASR、GET 合成 wav、声纹注册/验证、**浏览器麦克风 → WebSocket → 流式 ASR**（`/mica/voice/ws/online-asr`） |

> 后续将按需追加：
> - `mica-voice-example-mic-recorder`：独立 Recorder 模块（当前流式 ASR 已由 spring-web 的 WebSocket 实现覆盖）
> - `mica-voice-example-benchmark`：模型基准（WER / RTF / 内存）

## 状态

🚧 **v1.0.0-SNAPSHOT**（核心能力已就绪，持续迭代中）

- [x] 多模块仓库骨架（Phase 0）
- [x] mica-voice-core：ASR / TTS / 声纹 / VAD / 说话人分离 / 降噪 / KWS / Transcribe + 模型管理（Phase 1）
- [x] mica-voice-spring-boot-starter：自动装配，`mica.voice.*` 配置即开箱即用（Phase 2）
- [x] mica-voice-examples：console 纯 Java 示例（命令行测 ASR / SenseVoice / X-ASR / TTS / 声纹）
- [x] mica-voice-examples：spring-web 基础示例（REST 上传 wav 做 ASR、GET 合成 wav、声纹注册/验证、浏览器麦克风 → WebSocket → 流式 ASR）
- [ ] mica-voice-example-mic-recorder：独立 Recorder 模块（规划中，当前功能已由 spring-web 的 WebSocket 实现覆盖）
- [ ] mica-voice-example-benchmark：模型基准（WER / RTF / 内存）（规划中）

详细路线图见 [`docs/`](docs/)（即将创建）。

## 快速开始

```bash
# 克隆并下载模型
git clone https://github.com/lets-mica/mica-voice.git
cd mica-voice
bash models/scripts/download-models.sh   # Windows: models\scripts\download-models.bat（按需选 all 或单个目标）

# 方式一：控制台示例（纯 Java main，无需 Spring）
mvn -pl mica-voice-examples/mica-voice-example-console -am package -DskipTests
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar asr
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar sensevoice
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar xasr
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar tts
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar speaker

# 方式二：Spring Web 示例
cd mica-voice-examples/mica-voice-example-spring-web
mvn spring-boot:run
```

> 控制台示例默认从当前目录 `models/` 加载模型，也可用 `-Dmica.voice.models-dir=E:/.../models` 指定绝对路径。

## 关联项目

- [dreamlu/mica-sherpa-onnx](https://github.com/dreamlu/mica-sherpa-onnx) — sherpa-onnx 全平台 fat jar，本项目的 native 依赖来源
- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) — 上游 ONNX 推理引擎与模型

## License

Apache License 2.0 — 详见 [LICENSE](LICENSE)。