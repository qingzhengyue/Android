@echo off
chcp 65001 >nul
echo ==========================================
echo    1. 启动本地 Supabase 服务
echo ==========================================
echo.
supabase start

echo.
echo ==========================================
echo    2. 自动更新 Android 项目 IP 地址
echo ==========================================
echo.
python auto_update_ip.py

echo.
echo ==========================================
echo ✅ 启动并更新完毕！
echo 💡 你现在可以在 Android Studio 中运行项目了。
echo ==========================================
pause
