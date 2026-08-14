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

## 4. deploy 发布正式版，
mvn clean package deploy -P$profile -DskipTests -pl '!mica-voice-examples'

