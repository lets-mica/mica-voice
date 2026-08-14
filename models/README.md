# mica-voice 模型目录

此目录存放 mica-voice 运行时需要的模型文件，按需用下方脚本下载后即落盘于此。

## 已落盘模型

| 文件 / 目录 | 体积 | 用途 | 来源 |
| ----------- | ---- | ---- | ---- |
| `sherpa-onnx-paraformer-zh-small-2024-03-09/` | ~80MB | 离线 ASR（Paraformer，中+英，int8） | [asr-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models) |
| `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/` | ~230MB | 多语言离线 ASR（中/英/日/韩/粤 + 情感 + 音频事件，int8） | [HuggingFace csukuangfj](https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17) |
| `x-asr-zh-en-chunk-960ms/` | ~587MB | 在线流式 ASR（X-ASR，Zipformer2 transducer，960ms 分块） | [HuggingFace GilgameshWind](https://huggingface.co/GilgameshWind/X-ASR-zh-en) |
| `vits-icefall-zh-aishell3/` | ~204MB | TTS（VITS，单说话人） | [tts-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) |
| `3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx` | ~38MB | 声纹嵌入提取 | [speaker-recongition-models](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-recongition-models) |
| `0-four-speakers-zh.wav` | ~1.8MB | 测试音频（4 人对话，56.8 秒） | [speaker-segmentation-models](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-segmentation-models) |

## 可选模型（未落盘）

| 模型 | 用途 |
| ---- | ---- |
| `sherpa-onnx-streaming-paraformer-bilingual-zh-en/` | 流式 Paraformer（中英双语） |
| `sherpa-onnx-pyannote-segmentation-3-0.onnx` | 说话人分离 segmentation 模型 |
| `silero_vad.onnx` | VAD 语音活动检测模型 |

## 下载脚本

下载脚本统一存放在 `models/scripts/` 子目录（已从 `sherpa-onnx-demo/scripts/` 迁移至此）：

| 脚本 | 适用平台 | 说明 |
| ---- | -------- | ---- |
| `scripts/download-models.ps1` | Windows（推荐） | 主脚本：多段并行下载 + Range 断点续传 |
| `scripts/download-models.bat` | Windows | 快捷入口，自动转发到 `.ps1` |
| `scripts/download-models.sh` | Linux / macOS | bash 版，慢网时自动调用 `parallel-download.sh` |
| `scripts/parallel-download.sh` | Linux / macOS | 分段并行下载加速工具 |

脚本基于**自身所在目录**定位模型目录（`models/scripts/` 的父目录即 `models/`），下载即落盘，无需移动。

## 下载方式

```powershell
# Windows（仓库根目录执行，或先 cd 到 models/scripts/）
.\models\scripts\download-models.ps1 -Target sensevoice
.\models\scripts\download-models.ps1 -Target x-asr -Parts 6        # 指定并行段数

# 全部下载
.\models\scripts\download-models.ps1 -Target all

# 或直接运行 .bat（自动转发到 .ps1，参数一致）
.\models\scripts\download-models.bat sensevoice
```

```bash
# Linux / macOS（仓库根目录执行）
bash models/scripts/download-models.sh asr-sensevoice
bash models/scripts/download-models.sh x-asr
PARALLEL=8 bash models/scripts/download-models.sh tts   # 手动加大分段数加速
```

支持的目标：

| Target（ps1/bat） | sh 参数 | 内容 | 体积 |
| ---------------- | ------- | ---- | ---- |
| `sensevoice` | `asr-sensevoice` | SenseVoice 多语言离线 ASR（int8 + tokens + 5 语言测试音频） | ~228MB |
| `x-asr` | `x-asr` | X-ASR 中英流式 ASR（encoder/decoder/joiner + tokens） | ~586MB |
| `asr` | `asr` | 离线 Paraformer（zh small int8） | ~80MB |
| `asr-online` | `asr-online` | 流式 Paraformer（中英双语） | ~230MB |
| `tts` | `tts` | VITS icefall-aishell3 单说话人 | ~30MB |
| `tts-fanchen` | `tts-fanchen` | VITS fanchen-C 187 说话人 | ~290MB |
| `speaker` | `speaker` | 声纹模型 + 4 人测试音频 | ~95MB |
| `all` | `all` | 以上全部 | ~1.4GB |

> 提示：`sensevoice` / `x-asr` 走 HuggingFace（默认 hf-mirror.com 国内镜像，可用 `-Mirror huggingface` 或 `HF_BASE` 切换官方源），其余走 GitHub Releases。脚本支持多段并行与断点续传，已完整下载的文件自动跳过，中断后重跑即可续传。

## git 策略

- `models/` 下**模型文件不提交**（`.gitignore` 忽略 `models/*`，体积大，按需自行下载）；
- **README 与 `models/scripts/` 目录被正常跟踪**（`!models/scripts/` 例外规则），随仓库分发。

部署时把模型放在服务器 `models/` 目录下，配置文件中指定绝对路径即可（如 `mica.voice.models-dir: /opt/mica-voice/models`）。
