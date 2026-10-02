@echo off
setlocal

rem ================================================================
rem 更新 Windows Android SDK 工具，處理「SDK XML version 4」版本不一致警告。
rem 請先關閉正在執行的 Gradle Build，再以一般使用者身分執行本檔。
rem ================================================================

set "ANDROID_SDK_ROOT_LOCAL=%LOCALAPPDATA%\Android\Sdk"
set "SDK_MANAGER=%ANDROID_SDK_ROOT_LOCAL%\cmdline-tools\latest\bin\sdkmanager.bat"

if not exist "%SDK_MANAGER%" (
    echo [ERROR] 找不到 sdkmanager：
    echo %SDK_MANAGER%
    echo.
    echo 請開啟 Android Studio ^> Settings ^> Android SDK ^> SDK Tools，
    echo 勾選 Android SDK Command-line Tools ^(latest^) 後再執行本檔。
    pause
    exit /b 1
)

echo [1/3] 更新已安裝的 Android SDK 套件...
call "%SDK_MANAGER%" --update
if errorlevel 1 goto :failed

echo [2/3] 確認 API 36、Build Tools 35 與最新版命令列工具...
call "%SDK_MANAGER%" ^
    "cmdline-tools;latest" ^
    "platform-tools" ^
    "platforms;android-36" ^
    "build-tools;35.0.0"
if errorlevel 1 goto :failed

echo [3/3] 接受 Android SDK 授權條款...
call "%SDK_MANAGER%" --licenses
if errorlevel 1 goto :failed

echo.
echo [OK] Android SDK 工具更新完成。
echo 請重新啟動 Android Studio，再執行 check_gradle_warnings.bat。
pause
exit /b 0

:failed
echo.
echo [ERROR] Android SDK 工具更新失敗，請檢查網路或 Android Studio SDK 設定。
pause
exit /b 1
