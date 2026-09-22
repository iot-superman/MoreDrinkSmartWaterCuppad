@echo off
setlocal

rem 使用完整警告模式重新建置，確認是否仍有 Gradle 淘汰 API 警告。
call gradlew.bat clean build --warning-mode all

if errorlevel 1 (
    echo.
    echo [ERROR] Gradle 建置失敗，請查看上方第一個 error 訊息。
    pause
    exit /b 1
)

echo.
echo [OK] Gradle 建置完成。若上方沒有 Deprecated Gradle features，警告已排除。
pause
exit /b 0
