#!/bin/sh

## 0. java
if command -v vfox >/dev/null 2>&1; then
    vfox use java@8.0.342+7
else
    echo "Warning: vfox command not found, skipping Java version switch"
fi

## 1. java version
java -version
printf "\n"

## 2. maven version
mvn -version
printf "\n"

## 3. 环境
if [ -z $1 ]; then
    profile="release"
else
    profile="$1"
fi

## 4. deploy 发布正式版。
##    mica-voice-examples（聚合 + 两个 demo 子模块）是本地演示工程，不发布到仓库。
##    这里用正选 `-pl` 白名单（精确列举要发布的 artifactId），避免 `!` 黑名单
##    对聚合子模块不生效的陷阱；`-am` 顺带把依赖（mica-voice-core）也带上。
mvn clean package deploy -P$profile -DskipTests -am -pl ':mica-voice-core,:mica-voice-spring-boot-starter,:mica-voice-solon-plugin'

