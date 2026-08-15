#!/usr/bin/env bash
# =============================================================
# 下载 mica-sherpa-onnx 测试所需的模型与测试音频（bash 版）
# 用法: bash models/scripts/download-models.sh [TARGET]
#
# Target 一览：
#   asr               离线 ASR（Paraformer zh small int8, ~79MB）
#   asr-online        在线流式 ASR（Streaming Paraformer, ~230MB）
#   asr-sensevoice    SenseVoice 多语言离线 ASR（中英日韩粤+情感/事件, ~228MB）
#   x-asr             X-ASR 在线流式 ASR（Zipformer Transducer 960ms, ~586MB）
#   tts               TTS（VITS icefall-aishell3 单说话人, ~30MB）
#   tts-fanchen       TTS 大模型（VITS fanchen-C 187 说话人, ~290MB）
#   tts-zh-en         TTS 中英混合（VITS Melo + jieba + espeak, ~310MB）
#   speaker           声纹模型 + 测试音频（~95MB）
#   vad               SILERO VAD（v1.1, ~1MB，单文件）
#   denoise           GTCRN 降噪（v1.1, ~523KB，单文件）
#   denoise-dpdfnet   DPDFNet 降噪（v1.1, 高质量离线, ~数 MB）
#   kws               关键词唤醒（v1.1, ~30MB，解压后再补一份默认命名的副本）
#   diarization       说话人分离（v1.1, segmentation ~5.7MB, 解压后再补一份默认命名）
#   v11               vad + denoise + kws + diarization（v1.1 全部能力）
#   all               全部（含 v1.0 + v1.1）
#
# 默认 all
#
# 提示：GitHub / HuggingFace 直连较慢时，脚本会调用 models/scripts/parallel-download.sh
#       分段并行下载（默认 4 段），速度通常可提升数倍。
#       HuggingFace 走 hf-mirror.com 镜像（国内快），可用 HF_BASE 覆盖。
# =============================================================
set -e
# 脚本位于 models/scripts/ 下，向上两级回到仓库根（模型目录 = 仓库根/models）
cd "$(dirname "$0")/../.."
mkdir -p models output

GITHUB="https://github.com/k2-fsa/sherpa-onnx/releases/download"
HF_BASE="${HF_BASE:-https://hf-mirror.com}"
SENSE="sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17"
XASR="x-asr-zh-en-chunk-960ms"

download() { # $1=url $2=本地文件
  local url="$1" dest="$2"
  if [ -e "$dest" ]; then
    echo "[跳过] $dest 已存在"
    return
  fi
  mkdir -p "$(dirname "$dest")"
  echo "[下载] $url"
  if [ -f models/scripts/parallel-download.sh ]; then
    bash models/scripts/parallel-download.sh "$url" "$dest" "${PARALLEL:-4}"
  elif command -v curl >/dev/null 2>&1; then
    curl -L --retry 3 --continue-at - -o "$dest" "$url"
  elif command -v wget >/dev/null 2>&1; then
    wget -c -O "$dest" "$url"
  else
    echo "错误: 需要 curl 或 wget" >&2
    exit 1
  fi
}

unpack_bz2() { # $1=tar.bz2 文件，解压到 models/ 并删除压缩包
  local f="$1"
  echo "[解压] $f"
  tar -xjf "$f" -C models
  rm -f "$f"
}

# 等价于 PowerShell 的 Ensure-Like-File：解压后把 epoch/chunk 后缀文件复制成 mica-voice 默认期望的命名
ensure_like_file() { # $1=src $2=dst
  local src="$1" dst="$2"
  if [ -e "$src" ]; then
    if [ ! -e "$dst" ]; then
      cp -f "$src" "$dst"
      echo "  [link] $dst <- $src"
    else
      echo "  [跳过] $dst 已存在"
    fi
  else
    echo "  [警告] 源文件不存在，无法生成 $dst : $src"
  fi
}

case "${1:-all}" in
  asr)
    download "$GITHUB/asr-models/sherpa-onnx-paraformer-zh-small-2024-03-09.tar.bz2" \
      models/sherpa-onnx-paraformer-zh-small-2024-03-09.tar.bz2
    unpack_bz2 models/sherpa-onnx-paraformer-zh-small-2024-03-09.tar.bz2
    ;;
  asr-online)
    download "$GITHUB/asr-models/sherpa-onnx-streaming-paraformer-bilingual-zh-en.tar.bz2" \
      models/sherpa-onnx-streaming-paraformer-bilingual-zh-en.tar.bz2
    unpack_bz2 models/sherpa-onnx-streaming-paraformer-bilingual-zh-en.tar.bz2
    ;;
  asr-sensevoice)
    download "$HF_BASE/csukuangfj/$SENSE/resolve/main/model.int8.onnx" \
      "models/$SENSE/model.int8.onnx"
    download "$HF_BASE/csukuangfj/$SENSE/resolve/main/tokens.txt" \
      "models/$SENSE/tokens.txt"
    for w in zh yue en ja ko; do
      download "$HF_BASE/csukuangfj/$SENSE/resolve/main/test_wavs/$w.wav" \
        "models/$SENSE/test_wavs/$w.wav"
    done
    ;;
  x-asr)
    for f in encoder-960ms decoder-960ms joiner-960ms; do
      download "$HF_BASE/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/$f.onnx" \
        "models/$XASR/$f.onnx"
    done
    download "$HF_BASE/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/tokens.txt" \
      "models/$XASR/tokens.txt"
    ;;
  tts)
    download "$GITHUB/tts-models/vits-icefall-zh-aishell3.tar.bz2" \
      models/vits-icefall-zh-aishell3.tar.bz2
    unpack_bz2 models/vits-icefall-zh-aishell3.tar.bz2
    ;;
  tts-fanchen)
    download "$GITHUB/tts-models/vits-zh-hf-fanchen-C.tar.bz2" \
      models/vits-zh-hf-fanchen-C.tar.bz2
    unpack_bz2 models/vits-zh-hf-fanchen-C.tar.bz2
    ;;
  tts-zh-en)
    download "$GITHUB/tts-models/vits-melo-tts-zh_en.tar.bz2" \
      models/vits-melo-tts-zh_en.tar.bz2
    unpack_bz2 models/vits-melo-tts-zh_en.tar.bz2
    ;;
  speaker)
    download "$GITHUB/speaker-recongition-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx" \
      models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx
    download "$GITHUB/speaker-segmentation-models/0-four-speakers-zh.wav" \
      models/0-four-speakers-zh.wav
    ;;
  vad)
    # SILERO VAD 单文件，放 models/ 根
    download "$GITHUB/asr-models/silero_vad.onnx" models/silero_vad.onnx
    ;;
  denoise)
    # GTCRN 单文件，落盘为 mica-voice 默认名
    download "$GITHUB/speech-enhancement-models/gtcrn_simple.onnx" \
      models/sherpa-onnx-gtcrn.onnx
    ;;
  denoise-dpdfnet)
    # DPDFNet baseline（16kHz），高质量离线降噪
    download "$GITHUB/speech-enhancement-models/dpdfnet_baseline.onnx" \
      models/sherpa-onnx-dpdfnet.onnx
    ;;
  kws)
    # 关键词唤醒：下载 tarball，解压后再补一份 mica-voice 默认命名的副本
    #   KwsConfig 默认 modelDirName = sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01
    #   KwsConfig 默认 keywordsFile  = keywords.txt（位于 modelDirName 内）
    KWS_DIR="models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01"
    download "$GITHUB/kws-models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2" \
      models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2
    if [ -e models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2 ] \
       && [ ! -d "$KWS_DIR" ]; then
      unpack_bz2 models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2
    fi
    if [ -d "$KWS_DIR" ]; then
      ensure_like_file "$KWS_DIR/encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx" "$KWS_DIR/encoder.int8.onnx"
      ensure_like_file "$KWS_DIR/encoder-epoch-12-avg-2-chunk-16-left-64.onnx"      "$KWS_DIR/encoder.onnx"
      ensure_like_file "$KWS_DIR/decoder-epoch-12-avg-2-chunk-16-left-64.onnx"      "$KWS_DIR/decoder.onnx"
      ensure_like_file "$KWS_DIR/joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx"  "$KWS_DIR/joiner.int8.onnx"
      ensure_like_file "$KWS_DIR/joiner-epoch-12-avg-2-chunk-16-left-64.onnx"       "$KWS_DIR/joiner.onnx"
      ensure_like_file "$KWS_DIR/test_wavs/test_keywords.txt" "$KWS_DIR/keywords.txt"
    fi
    ;;
  diarization)
    # 说话人分离：segmentation 模型（pyannote-3.0）
    #   DiarizationConfig 默认 segmentationModelFileName = sherpa-onnx-pyannote-segmentation-3-0.onnx（位于 models/ 根）
    #   embedding 模型由 speaker target 负责
    download "$GITHUB/speaker-segmentation-models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2" \
      models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2
    if [ -e models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2 ] \
       && [ ! -d models/sherpa-onnx-pyannote-segmentation-3-0 ]; then
      unpack_bz2 models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2
    fi
    if [ -d models/sherpa-onnx-pyannote-segmentation-3-0 ]; then
      ensure_like_file models/sherpa-onnx-pyannote-segmentation-3-0/model.onnx      models/sherpa-onnx-pyannote-segmentation-3-0.onnx
      ensure_like_file models/sherpa-onnx-pyannote-segmentation-3-0/model.int8.onnx models/sherpa-onnx-pyannote-segmentation-3-0.int8.onnx
    fi
    ;;
  v11)
    # 一次性下完 v1.1 新增的 4 个能力（不重复 v1.0 已有模型）
    bash "$0" vad
    bash "$0" denoise
    bash "$0" kws
    bash "$0" diarization
    ;;
  all)
    bash "$0" asr
    bash "$0" asr-online
    bash "$0" asr-sensevoice
    bash "$0" x-asr
    bash "$0" tts
    bash "$0" speaker
    bash "$0" vad
    bash "$0" denoise
    bash "$0" kws
    bash "$0" diarization
    ;;
  *)
    echo "未知参数: $1" >&2
    echo "可选: asr / asr-online / asr-sensevoice / x-asr / tts / tts-fanchen / tts-zh-en /" >&2
    echo "      speaker / vad / denoise / denoise-dpdfnet / kws / diarization / v11 / all" >&2
    exit 1
    ;;
esac

echo
echo "完成！模型目录: $(cd models && pwd)"