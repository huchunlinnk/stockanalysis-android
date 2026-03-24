#!/bin/bash

# 演示部署脚本 - 展示部署流程
# 注意：此脚本用于演示，实际部署需要连接 Android 设备

set -e

echo "=================================="
echo "股票智能分析 Android 应用 - 演示部署"
echo "=================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}步骤 1/5: 检查项目结构...${NC}"
echo "-----------------------------------"

# 检查关键文件
FILES_TO_CHECK=(
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/example/stockanalysis/StockAnalysisApplication.kt"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/values/strings.xml"
)

for file in "${FILES_TO_CHECK[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ✓ $file"
    else
        echo -e "  ${RED}✗ $file (缺失)${NC}"
    fi
done

echo ""
echo -e "${BLUE}步骤 2/5: 统计项目文件...${NC}"
echo "-----------------------------------"

KOTLIN_FILES=$(find app/src -name "*.kt" 2>/dev/null | wc -l)
XML_LAYOUTS=$(find app/src -name "*.xml" -path "*/layout/*" 2>/dev/null | wc -l)
XML_RESOURCES=$(find app/src -name "*.xml" ! -path "*/layout/*" 2>/dev/null | wc -l)
DRAWABLES=$(find app/src -name "*.xml" -path "*/drawable/*" 2>/dev/null | wc -l)

echo "  Kotlin 源文件: $KOTLIN_FILES"
echo "  XML 布局文件: $XML_LAYOUTS"
echo "  XML 资源文件: $XML_RESOURCES"
echo "  Drawable 文件: $DRAWABLES"

echo ""
echo -e "${BLUE}步骤 3/5: 检查 Android SDK 环境...${NC}"
echo "-----------------------------------"

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "  ✓ Java: $JAVA_VERSION"
else
    echo -e "  ${YELLOW}! Java 未安装${NC}"
fi

if [ -n "$ANDROID_HOME" ]; then
    echo -e "  ✓ ANDROID_HOME: $ANDROID_HOME"
else
    echo -e "  ${YELLOW}! ANDROID_HOME 未设置${NC}"
fi

if command -v adb &> /dev/null; then
    ADB_VERSION=$(adb version | head -n 1)
    echo -e "  ✓ ADB: $ADB_VERSION"
else
    echo -e "  ${YELLOW}! ADB 未安装${NC}"
fi

echo ""
echo -e "${BLUE}步骤 4/5: 检查连接设备...${NC}"
echo "-----------------------------------"

if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -c "device$" || true)
    if [ "$DEVICES" -gt 0 ]; then
        echo -e "  ✓ 发现 $DEVICES 个设备"
        adb devices -l | grep -v "List of devices"
    else
        echo -e "  ${YELLOW}! 未检测到设备${NC}"
        echo "    请连接 Android 设备或启动模拟器"
    fi
else
    echo -e "  ${RED}✗ ADB 不可用${NC}"
fi

echo ""
echo -e "${BLUE}步骤 5/5: 项目构建状态...${NC}"
echo "-----------------------------------"

echo "  项目配置:"
echo "    - 应用 ID: com.example.stockanalysis"
echo "    - 最小 SDK: 24"
echo "    - 目标 SDK: 34"
echo "    - 版本: 1.0.0"
echo ""
echo "  依赖库:"
echo "    - Hilt (依赖注入)"
echo "    - Room (数据库)"
echo "    - Retrofit (网络)"
echo "    - WorkManager (后台任务)"
echo "    - MPAndroidChart (图表)"
echo "    - Glide (图片加载)"

echo ""
echo "=================================="
echo -e "${GREEN}项目检查完成!${NC}"
echo "=================================="
echo ""
echo "使用说明:"
echo "---------"
echo "1. 在 Android Studio 中打开项目:"
echo "   File -> Open -> StockAnalysisApp"
echo ""
echo "2. 等待 Gradle 同步完成"
echo ""
echo "3. 连接设备或启动模拟器"
echo ""
echo "4. 点击运行按钮 (▶)"
echo ""
echo "或使用命令行构建:"
echo "   ./gradlew assembleDebug"
echo ""
echo "然后安装 APK:"
echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
