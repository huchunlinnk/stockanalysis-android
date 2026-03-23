#!/bin/bash
# 资源修复脚本

cd /Users/chunlin5/opensource/stock-analysis/android/StockAnalysisApp/app/src/main/res

# 添加缺失的 style
cat >> values/themes.xml << 'EOF'

    <style name="CircleImageView" parent="">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">50%</item>
    </style>
EOF

# 添加缺失的字符串
cat >> values/strings.xml << 'EOF'
    <string name="edit">编辑</string>
    <string name="general_settings">通用设置</string>
    <string name="notifications">通知</string>
    <string name="notifications_desc">接收推送通知</string>
    <string name="dark_mode">深色模式</string>
    <string name="dark_mode_desc">切换深色主题</string>
    <string name="auto_refresh">自动刷新</string>
    <string name="auto_refresh_desc">自动刷新数据</string>
    <string name="analysis_settings">分析设置</string>
    <string name="default_analysis_type">默认分析类型</string>
    <string name="risk_preference">风险偏好</string>
    <string name="risk_preference_desc">调整风险偏好</string>
    <string name="conservative">保守</string>
    <string name="aggressive">激进</string>
    <string name="data_settings">数据设置</string>
    <string name="clear_cache">清除缓存</string>
    <string name="export_data">导出数据</string>
    <string name="about">关于</string>
    <string name="version">版本</string>
    <string name="privacy_policy">隐私政策</string>
    <string name="terms_of_service">服务条款</string>
    <string name="logout">退出登录</string>
EOF

# 创建缺失的图标
cat > drawable/ic_dark_mode.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.82 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z"/>
</vector>
EOF

cat > drawable/ic_refresh.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z"/>
</vector>
EOF

cat > drawable/ic_export.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M19,12v7H5v-7H3v7c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2zM13,12.67l2.59,-2.58L17,11.5l-5,5 -5,-5 1.41,-1.41L11,12.67V3h2v9.67z"/>
</vector>
EOF

echo "资源修复完成，请在 Android Studio 中重新编译"
