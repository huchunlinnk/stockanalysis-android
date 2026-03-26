#!/bin/bash

# 股票智能分析 Android 应用构建和安装脚本

set -e

echo "================================"
echo "股票智能分析 - Android 构建脚本"
echo "================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否连接了设备
check_device() {
    echo ""
    echo "检查设备连接..."
    
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}错误: 未找到 adb 命令${NC}"
        echo "请确保 Android SDK 已正确安装并添加到 PATH"
        exit 1
    fi
    
    DEVICE_COUNT=$(adb devices | grep -c "device$")
    
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}错误: 未检测到连接的设备${NC}"
        echo "请连接设备或启动模拟器后重试"
        exit 1
    fi
    
    echo -e "${GREEN}检测到 $DEVICE_COUNT 个设备${NC}"
    adb devices -l | grep -v "List of devices"
}

# 检查 Gradle
check_gradle() {
    echo ""
    echo "检查 Gradle..."
    
    if [ ! -f "./gradlew" ]; then
        echo -e "${RED}错误: 未找到 gradlew 脚本${NC}"
        exit 1
    fi
    
    # 确保 gradlew 可执行
    chmod +x ./gradlew
    
    echo -e "${GREEN}Gradle wrapper 就绪${NC}"
}

# 构建 Debug APK
build_apk() {
    echo ""
    echo "开始构建 Debug APK..."
    
    ./gradlew clean assembleDebug --no-daemon
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}构建成功!${NC}"
    else
        echo -e "${RED}构建失败!${NC}"
        exit 1
    fi
}

# 安装 APK
install_apk() {
    echo ""
    echo "安装 APK 到设备..."
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}错误: 未找到 APK 文件${NC}"
        exit 1
    fi
    
    adb install -r "$APK_PATH"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}安装成功!${NC}"
    else
        echo -e "${RED}安装失败!${NC}"
        exit 1
    fi
}

# 启动应用
launch_app() {
    echo ""
    echo "启动应用..."
    
    adb shell am start -n "com.example.stockanalysis/.ui.MainActivity"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}应用已启动!${NC}"
    fi
}

# 显示日志
show_logs() {
    echo ""
    read -p "是否查看应用日志? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "显示日志 (按 Ctrl+C 退出)..."
        adb logcat -c  # 清除旧日志
        adb logcat -s "StockAnalysis:D" "System.err:D" "AndroidRuntime:D" *:S
    fi
}

# 主流程
main() {
    echo ""
    echo "构建模式: Debug"
    echo "目标包名: com.example.stockanalysis"
    
    check_device
    check_gradle
    build_apk
    install_apk
    launch_app
    
    echo ""
    echo "================================"
    echo -e "${GREEN}部署完成!${NC}"
    echo "================================"
    
    show_logs
}

# 执行主流程
main
