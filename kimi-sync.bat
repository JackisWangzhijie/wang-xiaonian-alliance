@echo off
chcp 65001 >nul
REM Kimi 双分身同步系统 - Windows 快捷命令

cd /d "%~dp0"

if "%1"=="" goto help
if "%1"=="help" goto help
if "%1"=="init" goto init
if "%1"=="sync" goto sync
if "%1"=="setup" goto setup
if "%1"=="status" goto status
goto help

:help
echo.
echo  Kimi 双分身同步系统
echo  ===================
echo.
echo  用法: kimi-sync [command] [options]
echo.
echo  命令:
echo    init [repo-url]  初始化同步仓库
echo    sync             执行同步
echo    setup            设置自动同步
echo    status           查看同步状态
echo    help             显示帮助
echo.
echo  示例:
echo    kimi-sync init https://github.com/username/kimi-sync.git
echo    kimi-sync sync
echo    kimi-sync setup
echo.
goto end

:init
if "%2"=="" (
    echo 错误: 请提供仓库地址
    echo 用法: kimi-sync init https://github.com/username/kimi-sync.git
    goto end
)
python scripts\sync_windows.py init --repo %2
goto end

:sync
python scripts\sync_windows.py sync
goto end

:setup
python scripts\sync_windows.py setup
goto end

:status
echo.
echo  同步状态检查
echo  ============
echo.

if exist ".git" (
    echo [OK] Git 仓库已初始化
    git remote -v 2>nul | findstr "origin" && echo [OK] 远程仓库已配置 || echo [WARN] 远程仓库未配置
) else (
    echo [ERR] Git 仓库未初始化，请先运行: kimi-sync init [repo-url]
)

echo.
echo 本地设备: 
findstr "device_id" sync.conf 2>nul || echo 未配置
echo.

echo 共享目录:
echo   - Skills: shared\skills
echo   - Tools:  shared\tools
echo   - Memory: shared\memory
dir shared\* /b 2>nul | find /c "/" >nul && echo [OK] 共享目录存在 || echo [WARN] 共享目录为空
echo.

goto end

:end
