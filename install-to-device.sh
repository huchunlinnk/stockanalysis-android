#!/bin/bash

# 安装脚本 - 需要在 Android Studio 中构建后才能使用

set -e

echo "=================================="
echo "股票智能分析 - 设备安装脚本"
echo "=================================="
echo ""

# 检查设备
echo "检查设备连接..."
adb devices

if [ $? -ne 0 ]; then
    echo "错误: adb 不可用"
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -c "device$")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "错误: 未检测到设备"
    echo "请连接设备并开启 USB 调试"
    exit 1
fi

echo "检测到 $DEVICE_COUNT 个设备"
echo ""

# 检查 APK 是否存在
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "错误: 未找到 APK 文件"
    echo "请先使用 Android Studio 构建项目:"
    echo "  Build → Build Bundle(s) / APK(s) → Build APK(s)"
    echo ""
    echo "或者使用 gradle 命令:"
    echo "  ./gradlew assembleDebug"
    exit 1
fi

echo "找到 APK: $APK_PATH"
echo ""

# 安装 APK
echo "正在安装..."
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 安装成功!"
    echo ""
    echo "启动应用..."
    adb shell am start -n "com.example.stockanalysis/.ui.MainActivity"
else
    echo ""
    echo "❌ 安装失败"
    exit 1
fi

echo ""
echo "=================================="
echo "完成!"
echo "=================================="
