#!/usr/bin/env bash
# =============================================================
# 下载 mica-sherpa-onnx 测试所需的模型与测试音频（bash 版）
# 用法: bash models/scripts/download-models.sh [asr|asr-online|asr-sensevoice|x-asr|tts|tts-fanchen|speaker|all]
#   默认 all: 下载全部
#   asr:            仅离线 ASR 模型（Paraformer zh small int8, ~79MB）
#   asr-online:     仅在线流式 ASR 模型（Streaming Paraformer, ~230MB）
#   asr-sensevoice: SenseVoice 多语言离线 ASR（中英日韩粤 + 情感/事件, ~228MB，来自 HuggingFace）
#   x-asr:          X-ASR 在线流式 ASR（Zipformer Transducer 960ms, ~586MB，来自 HuggingFace）
#   tts:            仅 TTS 模型（VITS icefall-aishell3 单说话人, ~30MB，轻量）
#   tts-fanchen:    TTS 大模型（VITS fanchen-C 187 说话人, ~290MB）
#   speaker:        仅声纹模型 + 测试音频（~95MB）
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
  if [ -f scripts/parallel-download.sh ]; then
    bash scripts/parallel-download.sh "$url" "$dest" "${PARALLEL:-4}"
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
  speaker)
    download "$GITHUB/speaker-recongition-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx" \
      models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx
    download "$GITHUB/speaker-segmentation-models/0-four-speakers-zh.wav" \
      models/0-four-speakers-zh.wav
    ;;
  all)
    bash "$0" asr
    bash "$0" asr-online
    bash "$0" asr-sensevoice
    bash "$0" x-asr
    bash "$0" tts
    bash "$0" speaker
    ;;
  *)
    echo "未知参数: $1（可选 asr / asr-online / asr-sensevoice / x-asr / tts / tts-fanchen / speaker / all）" >&2
    exit 1
    ;;
esac

echo
echo "完成！模型目录: $(cd models && pwd)"
