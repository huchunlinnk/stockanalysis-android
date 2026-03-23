#!/bin/bash
# StockAnalysisApp 一键部署脚本

set -e

echo "=== StockAnalysisApp 部署脚本 ==="
echo ""

# 检查设备连接
echo "[1/5] 检查设备连接..."
adb devices
if [ -z "$(adb devices | grep -v 'List' | grep 'device')" ]; then
    echo "❌ 未检测到设备，请检查 USB 连接"
    exit 1
fi
echo "✅ 设备已连接"
echo ""

# 进入项目目录
cd "$(dirname "$0")"

# 卸载旧版本
echo "[2/5] 卸载旧版本..."
adb uninstall com.example.stockanalysis 2>/dev/null || true
echo "✅ 卸载完成"
echo ""

# 安装 APK
echo "[3/5] 安装 APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 文件不存在，请先编译: ./gradlew assembleDebug"
    exit 1
fi
adb install -t -r "$APK_PATH"
echo "✅ 安装成功"
echo ""

# 启动应用
echo "[4/5] 启动应用..."
adb shell am start -n com.example.stockanalysis/.ui.MainActivity
echo "✅ 应用已启动"
echo ""

# 验证运行
echo "[5/5] 验证运行状态..."
sleep 2
if adb shell ps | grep -q stockanalysis; then
    echo "✅ 应用正在运行"
    echo ""
    echo "=== 部署成功 ==="
    echo "应用包名: com.example.stockanalysis"
    echo "启动 Activity: com.example.stockanalysis/.ui.MainActivity"
    echo ""
    echo "查看日志命令:"
    echo "  adb logcat -s stockanalysis:D"
else
    echo "⚠️ 应用可能未正常运行，请检查日志"
    echo ""
    echo "查看日志命令:"
    echo "  adb logcat | grep stockanalysis"
fi
