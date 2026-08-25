# mica-voice 模型目录

此目录存放 mica-voice 运行时需要的模型文件，按需用下方脚本下载后即落盘于此。

## 模型选择

下表列出 mica-voice 支持的所有模型，按需用对应脚本 target 或自行下载到 `models/`。
⭐ = 同场景下更推荐；同类中仅推荐一个，避免选择负担。

| 文件 / 目录 | 体积 | 用途 | 脚本 target | 来源 |
| ----------- | ---- | ---- | ----------- | ---- |
| `sherpa-onnx-paraformer-zh-small-2024-03-09/` | ~80MB | 离线 ASR（Paraformer，中+英，int8，**默认**） | `asr` | [asr-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models) |
| ⭐ `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/` | ~230MB | **离线 ASR（多语言 / 情感 / 音频事件，效果更好）** | `asr-sensevoice` / `sensevoice` | [HuggingFace csukuangfj](https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17) |
| `sherpa-onnx-streaming-paraformer-bilingual-zh-en/` | ~230MB | 流式 Paraformer（中英双语） | `asr-online` | [asr-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models) |
| ⭐ `x-asr-zh-en-chunk-960ms/` | ~587MB | **在线流式 ASR（X-ASR，Zipformer2 transducer，960ms 分块，效果更好）** | `x-asr` | [HuggingFace GilgameshWind](https://huggingface.co/GilgameshWind/X-ASR-zh-en) |
| ⭐ `vits-icefall-zh-aishell3/` | ~204MB | **TTS（VITS，单说话人，纯中文首选，体积最小）** | `tts` | [tts-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) |
| `vits-zh-hf-fanchen-C/` | ~290MB | TTS（VITS fanchen-C，187 说话人） | `tts-fanchen` | [tts-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) |
| `vits-melo-tts-zh_en/` | ~310MB | TTS（VITS Melo，中英混合，jieba + espeak-ng） | `tts-zh-en` | [tts-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) |
| ⭐ `3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx` | ~38MB | **声纹嵌入提取（同时供 speaker / diarization 使用）** | `speaker` | [speaker-recongition-models](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-recongition-models) |
| `0-four-speakers-zh.wav` | ~1.8MB | 测试音频（4 人对话，56.8 秒） | `speaker` | [speaker-segmentation-models](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-segmentation-models) |
| ⭐ `sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01/` | ~30MB | **关键词唤醒（Zipformer Transducer, WenetSpeech 中文）** | `kws` | [kws-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/kws-models) |
| ⭐ `sherpa-onnx-pyannote-segmentation-3-0.onnx` | ~5.7MB | **说话人分离 segmentation（fp32，效果更准）** | `diarization` | [speaker-segmentation-models](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-segmentation-models) |
| `sherpa-onnx-pyannote-segmentation-3-0.int8.onnx` | ~1.5MB | 说话人分离 segmentation（int8，体积更小） | `diarization` | 同上 |
| ⭐ `sherpa-onnx-gtcrn.onnx` | ~523KB | **音频降噪（GTCRN，极轻量、流式友好）** | `denoise` | [speech-enhancement-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speech-enhancement-models) |
| `sherpa-onnx-dpdfnet.onnx` | 数 MB | Denoise DPDFNet（高质量离线降噪，体积大、CPU 慢） | `denoise-dpdfnet` | [speech-enhancement-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/speech-enhancement-models) |
| ⭐ `silero_vad.onnx` | ~1MB | **语音活动检测（SILERO VAD，CPU 友好，默认）** | `vad` | [asr-models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models) |
| `ten_vad.onnx` | ~数 MB | TEN VAD | 自行下载到 `models/ten_vad.onnx` | [TenVad](https://github.com/TencentGameMate/chinese_speech_pretrain) |

> 选择指南：
> - **离线 ASR**：纯中文 + 极致速度 → Paraformer-zh-small；需要多语 / 情感 / 事件 → ⭐ SenseVoice（需要在 yml 里把 `mica.voice.asr.offline.model-type` 改为 `SENSE_VOICE`）。
> - **在线 ASR**：低延迟 + 中英混合 → ⭐ X-ASR（默认推荐）。
> - **TTS**：纯中文场景首选 ⭐ `vits-icefall-zh-aishell3`；中英混合 → `vits-melo-tts-zh_en`；要 187 说话人挑音色 → `tts-fanchen`。
> - **降噪**：CPU / 实时场景首选 ⭐ GTCRN（~523KB，RTF ≈ 0.07）；追求最高音质且不在乎 CPU → DPDFNet。
> - **说话人分离**：CPU 推荐 ⭐ int8 版；GPU / 服务端追求精度 → fp32 版。
> - **VAD**：默认 ⭐ SILERO 即可（CPU 友好，~1MB）。

## 下载脚本

下载脚本统一存放在 `models/scripts/` 子目录（已从 `sherpa-onnx-demo/scripts/` 迁移至此）：

| 脚本 | 适用平台 | 说明 |
| ---- | -------- | ---- |
| `scripts/download-models.ps1` | Windows（推荐） | 主脚本：多段并行下载 + Range 断点续传 |
| `scripts/download-models.bat` | Windows | 快捷入口，自动转发到 `.ps1` |
| `scripts/download-models.sh` | Linux / macOS | bash 版，慢网时自动调用 `parallel-download.sh` |
| `scripts/parallel-download.sh` | Linux / macOS | 分段并行下载加速工具 |

### 脚本支持的目标（target）一览

| Target | 脚本对应 Mica 能力 | 说明 |
| ------ | ----------------- | ---- |
| `asr` | offline ASR (Paraformer zh) | sherpa-onnx Paraformer int8，~80MB |
| `asr-online` | online ASR (Streaming Paraformer) | 流式 Paraformer 中英，~230MB |
| `asr-sensevoice` | offline ASR (SenseVoice 多语言) | 中/英/日/韩/粤 + 情感/事件，~228MB |
| `x-asr` | online ASR (X-ASR 960ms chunk) | Zipformer2 Transducer，~586MB |
| `tts` | TTS (VITS icefall-aishell3) | 单说话人，~30MB |
| `tts-fanchen` | TTS (VITS fanchen-C) | 187 说话人大模型，~290MB |
| `tts-zh-en` | TTS (VITS Melo 中英) | melo-tts 中英混合（jieba + espeak），~310MB |
| `speaker` | 声纹识别 | embedding 模型 + 4 人测试音频，~95MB |
| `vad` | VAD | SILERO VAD，~1MB，单文件落到 `models/silero_vad.onnx` |
| `denoise` | Denoise GTCRN | ~523KB，落盘为 `models/sherpa-onnx-gtcrn.onnx` |
| `denoise-dpdfnet` | Denoise DPDFNet | 高质量离线降噪，落盘为 `models/sherpa-onnx-dpdfnet.onnx` |
| `kws` | 关键词唤醒 | sherpa-onnx Zipformer KWS（WenetSpeech），~30MB；解压后会自动把 `encoder-epoch-…-chunk-16-left-64.{int8.,}onnx` 等真名文件复制成 mica-voice 默认期望的 `encoder/decoder/joiner.onnx` + `keywords.txt` |
| `diarization` | 说话人分离 | pyannote-segmentation-3.0，~5.7MB；解压后会把 `model.onnx` / `model.int8.onnx` 复制成 mica-voice 默认期望的 `sherpa-onnx-pyannote-segmentation-3-0.onnx` 等单文件（embedding 模型复用 `speaker` target 下的 `3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx`） |
| `all` | 全部 | 上述之和，~1.5GB |

> 全部 target 都已并入 `all`，执行 `download-models.{ps1,sh,bat} -Target all` 即可一键落盘。
> 不需要的能力直接从命令里去掉对应的 target 即可。

脚本基于**自身所在目录**定位模型目录（`models/scripts/` 的父目录即 `models/`），下载即落盘，无需移动。

## 下载方式

```powershell
# Windows（仓库根目录执行，或先 cd 到 models/scripts/）
.\models\scripts\download-models.ps1 -Target sensevoice
.\models\scripts\download-models.ps1 -Target x-asr -Parts 6        # 指定并行段数

# 新增能力（VAD / 降噪 / 关键词唤醒 / 说话人分离）
.\models\scripts\download-models.ps1 -Target vad
.\models\scripts\download-models.ps1 -Target denoise
.\models\scripts\download-models.ps1 -Target kws
.\models\scripts\download-models.ps1 -Target diarization

# 全部下载
.\models\scripts\download-models.ps1 -Target all

# 或直接运行 .bat（自动转发到 .ps1，参数一致）
.\models\scripts\download-models.bat sensevoice
```

```bash
# Linux / macOS（仓库根目录执行）
bash models/scripts/download-models.sh asr-sensevoice
bash models/scripts/download-models.sh x-asr
bash models/scripts/download-models.sh vad
bash models/scripts/download-models.sh kws
PARALLEL=8 bash models/scripts/download-models.sh tts                  # 手动加大分段数加速
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
| `tts-zh-en` | `tts-zh-en` | VITS Melo 中英混合 | ~310MB |
| `speaker` | `speaker` | 声纹模型 + 4 人测试音频 | ~95MB |
| `vad` | `vad` | SILERO VAD | ~1MB |
| `denoise` | `denoise` | GTCRN 降噪 | ~523KB |
| `denoise-dpdfnet` | `denoise-dpdfnet` | DPDFNet 高质量离线降噪 | 数 MB |
| `kws` | `kws` | KWS 关键词唤醒（WenetSpeech） | ~30MB |
| `diarization` | `diarization` | 说话人分离 pyannote-segmentation-3.0 | ~5.7MB |
| `all` | `all` | 以上全部 | ~1.5GB |

> 镜像说明：`sensevoice` / `x-asr` 走 HuggingFace，**默认 `hf-mirror.com`（HuggingFace 官方内容的国内反代镜像，国内无需科学上网）**。
> 切换回官方源：
>   - PowerShell：`-Mirror huggingface`
>   - bash：`HF_BASE=https://huggingface.co bash models/scripts/download-models.sh sensevoice`
>
> ModelScope（魔搭）暂未提供这两个模型的官方同步仓库，所以脚本没有 ModelScope 选项。其余目标走 GitHub Releases。
> 脚本支持多段并行与断点续传，已完整下载的文件自动跳过，中断后重跑即可续传。

**网盘下载：**

链接（夸克网盘）：https://pan.quark.cn/s/d2da0116a472?pwd=kc5e
提取码：kc5e

## git 策略

- `models/` 下**模型文件不提交**（`.gitignore` 忽略 `models/*`，体积大，按需自行下载）；
- **README 与 `models/scripts/` 目录被正常跟踪**（`!models/scripts/` 例外规则），随仓库分发。

部署时把模型放在服务器 `models/` 目录下，配置文件中指定绝对路径即可（如 `mica.voice.models-dir: /opt/mica-voice/models`）。

## 模型目录与文件命名规范（按能力）

下面按 mica-voice 的 7 类能力，详细列出每类模型在 `models/` 下的目录布局与具体文件命名。
所有路径都相对于 `mica.voice.models-dir`（默认 `./models`）。配置里写的 `model-dir-name` /
`model-file-name` / `model-candidates` 等就是这些路径的**最后一段**。

约定：
- `dir=` 表示一个子目录（`model-dir-name` 配置项）；
- `file=` 表示单文件（`model-file-name` 配置项）；
- `*` 表示可选/候选（SDK 会按候选名顺序找第一个存在的）。

### 1. 离线 ASR（`mica.voice.asr.offline`）

配置项：`mica.voice.asr.offline.{model-dir-name, model-type, language, inverse-text-normalization, ...}`

`model-type` 取值：`PARAFORMER`（默认） / `SENSE_VOICE` / `WHISPER` / `MOONSHINE` / `ZIPFORMER` / `NEMO_CTC` / `AUTO`。

目录布局：

```
models/
└── dir=sherpa-onnx-paraformer-zh-small-2024-03-09/      # offline.model-dir-name
    ├── model.onnx                                       # 主模型（必填，PARAFORMER）
    ├── model.int8.onnx                                  # int8 量化版（部分模型，SDK 自动选）
    ├── tokens.txt                                       # 词表（必填）
    ├── config.yaml                                      # 可选，部分模型（Zipformer / NeMo CTC）需要
    ├── *.json                                           # 可选，SenseVoice 的 config.json / 多语言 meta
    └── test_wavs/                                       # 可选，部分发布包附带测试音频
        └── *.wav
```

不同 `model-type` 期望的文件：

| model-type | 必填/常见文件 | 备注 |
| ---------- | -------------- | ---- |
| `PARAFORMER` | `model.onnx`（或 `model.int8.onnx`）+ `tokens.txt` | Paraformer-zh / zh-en 等 |
| `SENSE_VOICE` | `model.onnx` + `tokens.txt` + `config.json`（可选）+ 测试音频（可选） | 多语言 + 情感 + 事件；支持 `language` / `inverse-text-normalization` |
| `WHISPER` | `model.onnx` + `tokens.txt` | 多语言；`language` 生效 |
| `MOONSHINE` | `model.onnx` + `tokens.txt` | 轻量 Whisper 替代 |
| `ZIPFORMER` | `encoder.onnx` + `decoder.onnx` + `joiner.onnx` + `tokens.txt` | 非流式 Zipformer；与在线的不同（在线是 `*-960ms.onnx`） |
| `NEMO_CTC` | `model.onnx` + `tokens.txt` + `config.yaml`（可选） | NeMo CTC |
| `AUTO` | 任意上述组合 | 由 sherpa-onnx 自动推断模型家族 |

> yml 中 `model-dir-name` 写的就是上面 `dir=` 那层目录名。

### 2. 在线（流式）ASR（`mica.voice.asr.online`）

配置项：`mica.voice.asr.online.{model-dir-name, model-type, chunk-size, endpoint-*, ...}`

`model-type` 取值：`PARAFORMER` / `X_ASR` / `ZIPFORMER` / `ZIPFORMER2_CTC` / `NEMO_CTC` / `TRANSDUCER` / `AUTO`。

目录布局（Transducer 三段式：X_ASR / ZIPFORMER / ZIPFORMER2_CTC / TRANSDUCER）：

```
models/
└── dir=x-asr-zh-en-chunk-960ms/                         # online.model-dir-name
    ├── encoder-960ms.onnx                               # 候选 1（X-ASR 专用名）
    ├── encoder.int8.onnx                                # 候选 2
    ├── encoder.onnx                                     # 候选 3
    ├── decoder-960ms.onnx
    ├── decoder.int8.onnx
    ├── decoder.onnx
    ├── joiner-960ms.onnx                                # X_ASR / Transducer 必填
    ├── joiner.int8.onnx
    ├── joiner.onnx
    └── tokens.txt
```

目录布局（流式 Paraformer，两段式 encoder + decoder）：

```
models/
└── dir=sherpa-onnx-streaming-paraformer-bilingual-zh-en/
    ├── encoder.onnx                                     # 或 encoder.int8.onnx
    ├── decoder.onnx                                     # 或 decoder.int8.onnx
    └── tokens.txt
```

`encoder-candidates` / `decoder-candidates` / `joiner-candidates` 的默认优先级：

```
encoder: encoder-960ms.onnx → encoder.int8.onnx → encoder.onnx
decoder: decoder-960ms.onnx → decoder.int8.onnx → decoder.onnx
joiner : joiner-960ms.onnx  → joiner.int8.onnx  → joiner.onnx
```

可在 yml 里覆盖：

```yaml
mica:
  voice:
    asr:
      online:
        encoder-candidates: [encoder-960ms.onnx, encoder.int8.onnx, encoder.onnx]
```

### 3. TTS（`mica.voice.tts`）

配置项：`mica.voice.tts.{model-dir-name, model-type, default-speaker-id, default-speed, callback-sample-step, ...}`

`model-type` 取值：`VITS`（默认） / `MATCHA` / `KOKORO` / `AUTO`。

目录布局（VITS / Melo 通用）：

```
models/
└── dir=vits-icefall-zh-aishell3/                        # tts.model-dir-name
    ├── model.onnx                                       # 主模型（必填）
    ├── model.int8.onnx                                  # 可选 int8 版本
    ├── tokens.txt                                       # 词表（必填）
    ├── lexicon.txt                                      # 发音词典（icefall 系必填，melo 系可选）
    ├── espeak-ng-data/                                  # 可选：melo-tts 中英混合必需
    │   └── phonindex, phondata, ...
    ├── dict/                                            # 可选：icefall 系发音字典目录
    │   └── *.txt
    └── tokens.txt                                       # 多说话人时同一 tokens，多 speaker 嵌入在 model.onnx 里
```

> Melo 中英（`vits-melo-tts-zh_en`）还需要 `espeak-ng-data/`；纯中文 icefall 模型还会带 `dict/`。
> SDK 会自动检测 `lexicon.txt` / `dict/` / `espeak-ng-data/` 并设置 `dataDir / dictDir`。

### 4. 声纹识别 Speaker（`mica.voice.speaker`）

配置项：`mica.voice.speaker.{model-candidates, threshold, embedding-timeout-ms, ...}`

模型是**单文件** ONNX，默认放在 `models/` 根下（也可用同名子目录）。SDK 按候选名顺序找。

目录布局：

```
models/
├── file=3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx   # 默认
├── file=3dspeaker_speech_eres2net_base_sv_zh_en.onnx                 # 候选
├── file=3dspeaker_speech_campplus_sv_zh_en.onnx                      # 候选
└── 0-four-speakers-zh.wav                                            # 测试音频（可选）
```

`model-candidates` 默认顺序：

```
3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx
3dspeaker_speech_eres2net_base_sv_zh_en.onnx
3dspeaker_speech_campplus_sv_zh_en.onnx
```

yml 示例（按需替换/添加）：

```yaml
mica:
  voice:
    speaker:
      model-candidates:
        - 3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx
        - my_custom_embedding.onnx
```

### 5. VAD（`mica.voice.vad`）

配置项：`mica.voice.vad.{model-file-name, model-type, sample-rate, threshold, min-silence-duration, min-speech-duration, max-speech-duration, window-size, ...}`

`model-type` 取值：`SILERO`（默认） / `TEN`。模型是**单文件**，放在 `models/` 根下。

目录布局：

```
models/
└── file=silero_vad.onnx                                 # vad.model-file-name（SILERO，~1MB）
# 或
└── file=ten_vad.onnx                                    # TEN
```

### 6. 关键词唤醒 KWS（`mica.voice.kws`）

配置项：`mica.voice.kws.{model-dir-name, keywords-file, sample-rate, feature-dim, keywords-score, keywords-threshold, max-active-paths, ...}`

目录布局：

```
models/
└── dir=sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01/          # kws.model-dir-name
    ├── encoder.onnx                                                    # 或 encoder.int8.onnx
    ├── decoder.onnx                                                    # 或 decoder.int8.onnx
    ├── joiner.onnx                                                     # 或 joiner.int8.onnx
    ├── tokens.txt
    └── keywords.txt                                                   # kws.keywords-file（每行: "关键词 tokens 序列"）
```

> `keywords-file` 默认 `keywords.txt`，位于 `model-dir-name` 内（不是 `models/` 根）。
> yml 里可改 `keywords-file` 来指向自定义词表。

### 7. 说话人分离 Diarization（`mica.voice.diarization`）

配置项：`mica.voice.diarization.{segmentation-model-file-name, embedding-model-file-name, num-clusters, cluster-threshold, min-duration-off, min-duration-on, ...}`

Diarization 由 segmentation + embedding 两个 ONNX 组成（均为单文件），都放在 `models/` 根下。

目录布局：

```
models/
├── file=sherpa-onnx-pyannote-segmentation-3-0.onnx                     # segmentation-model-file-name
├── file=3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx      # embedding-model-file-name
└── ...
```

> `embedding-model-file-name` 与 speaker 用的嵌入模型**通常同一个**，可复用 `models/` 根下已有的那份。

### 8. 音频降噪 Denoise（`mica.voice.denoise`）

配置项：`mica.voice.denoise.{model-file-name, model-type, attenuation-limit-db, ...}`

`model-type` 取值：`GTCRN`（默认） / `DPDFNet`。模型是**单文件**，放在 `models/` 根下。

目录布局：

```
models/
└── file=sherpa-onnx-gtcrn.onnx                                         # denoise.model-file-name（GTCRN，~1MB）
# 或
└── file=sherpa-onnx-dpdfnet.onnx                                        # DeepFilterNet3，~5MB
```

---

### 通用速查表

| 能力 | yml 段 | 模型放哪 | 关键命名 |
| ---- | ------ | -------- | -------- |
| 离线 ASR | `mica.voice.asr.offline` | `models/<model-dir-name>/` | `model.onnx` + `tokens.txt`（部分带 `*.json` / `config.yaml`） |
| 在线 ASR | `mica.voice.asr.online` | `models/<model-dir-name>/` | `encoder*onnx` + `decoder*onnx`（+ `joiner*onnx`）+ `tokens.txt` |
| TTS | `mica.voice.tts` | `models/<model-dir-name>/` | `model.onnx` + `tokens.txt`（+ `lexicon.txt` / `dict/` / `espeak-ng-data/`） |
| 声纹 | `mica.voice.speaker` | `models/` 根下 | `*eres2net*.onnx` / `*campplus*.onnx` |
| VAD | `mica.voice.vad` | `models/` 根下 | `silero_vad.onnx` / `ten_vad.onnx` |
| KWS | `mica.voice.kws` | `models/<model-dir-name>/` | encoder/decoder/joiner + `tokens.txt` + `keywords.txt` |
| Diarization | `mica.voice.diarization` | `models/` 根下 | segmentation `.onnx` + embedding `.onnx` |
| Denoise | `mica.voice.denoise` | `models/` 根下 | `sherpa-onnx-gtcrn.onnx` / `sherpa-onnx-dpdfnet.onnx` |

> 单文件类（speaker / vad / diarization 的两个模型 / denoise）默认直接放在 `models/` 根目录；
> 如需分目录放置，把 yml 中的 `model-file-name` / `segmentation-model-file-name` /
> `embedding-model-file-name` 改成相对 `models-dir` 的子路径即可（如 `vad/silero_vad.onnx`）。
