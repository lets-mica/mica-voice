#!/usr/bin/env bash
# 分段并行下载 —— GitHub 直连慢（<100KB/s）时的加速方案。
#
# GitHub Releases 支持 HTTP Range 请求，本脚本把文件切成 N 段同时下载，
# 再把各段拼接成完整文件，通常可把下载速度提升 N 倍。
#
# 用法: bash parallel-download.sh <url> <输出文件> [分段数]
#   分段数 默认 4，可传 8/16，越大越快但需注意网络稳定性。
#
# 依赖: curl（支持 -r Range）
set -uo pipefail

URL="$1"
OUT="$2"
N="${3:-4}"

# 获取文件总大小
SIZE=$(curl -sIL --retry 3 "$URL" | grep -iE '^content-length:' | tail -1 | tr -d '\r' | awk '{print $2}')
if [ -z "$SIZE" ] || [ "$SIZE" -le 0 ]; then
  echo "[错误] 无法获取文件大小: $URL" >&2
  exit 1
fi
echo "文件大小: $((SIZE / 1024 / 1024)) MB，分段数: $N"

CHUNK=$(( (SIZE + N - 1) / N ))
PIDS=()
for i in $(seq 0 $((N - 1))); do
  START=$((i * CHUNK))
  END=$(( (i + 1) * CHUNK - 1 ))
  [ "$END" -ge "$SIZE" ] && END=$((SIZE - 1))
  EXPECT=$((END - START + 1))
  # 断点续传：分段已完整则跳过
  if [ -f "$OUT.part$i" ] && [ "$(wc -c < "$OUT.part$i" | tr -d ' ')" -eq "$EXPECT" ]; then
    echo "  第 $i 段已完整，跳过"
    continue
  fi
  # --max-time 600: 单段最多 10 分钟
  # --connect-timeout 30: 连接超时 30 秒
  # --speed-limit 1024 --speed-time 30: 30 秒内低于 1KB/s 则中止（防卡死）
  curl -sL --retry 5 --retry-delay 2 --max-time 600 --connect-timeout 30 \
       --speed-limit 1024 --speed-time 30 \
       -r "$START-$END" -o "$OUT.part$i" "$URL" &
  PIDS+=($!)
done

FAIL=0
for pid in "${PIDS[@]}"; do
  wait "$pid" || FAIL=1
done
if [ "$FAIL" -ne 0 ]; then
  echo "[错误] 下载失败（分段断线），请重试" >&2
  exit 1
fi

# 校验各段大小并合并
for i in $(seq 0 $((N - 1))); do
  START=$((i * CHUNK))
  END=$(( (i + 1) * CHUNK - 1 ))
  [ "$END" -ge "$SIZE" ] && END=$((SIZE - 1))
  EXPECT=$((END - START + 1))
  ACTUAL=$(wc -c < "$OUT.part$i" | tr -d ' ')
  if [ "$ACTUAL" -ne "$EXPECT" ]; then
    echo "[错误] 第 $i 段大小不符: 期望 $EXPECT 实际 $ACTUAL" >&2
    exit 1
  fi
done
cat "$OUT".part* > "$OUT"
rm -f "$OUT".part*
echo "完成: $OUT"
