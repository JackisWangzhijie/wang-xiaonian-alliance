@echo off
chcp 65001 >nul
REM 王小年联盟 - Windows 快捷命令

cd /d "%~dp0"

if "%1"=="" goto help
if "%1"=="chat" goto chat
if "%1"=="send" goto send
if "%1"=="read" goto read
if "%1"=="task" goto task
if "%1"=="status" goto status
if "%1"=="help" goto help
goto help

:chat
if "%2"=="send" goto chat_send
if "%2"=="read" goto chat_read
if "%2"=="status" goto chat_status
echo [ERR] 未知聊天命令
goto chat_help

:chat_send
if "%3"=="" (
    echo [ERR] 请提供消息内容
    goto chat_help
)
python wxn-chat.py send -m "%3" -c "%4" 2>nul || python wxn-chat.py send -m "%3"
goto end

:chat_read
python wxn-chat.py read -c "%3" -n "%4" 2>nul || python wxn-chat.py read
goto end

:chat_status
python wxn-chat.py status
goto end

:chat_help
echo.
echo  聊天命令用法:
echo    wxn chat send "消息内容" [频道]
echo    wxn chat read [频道] [数量]
echo    wxn chat status
echo.
echo  示例:
echo    wxn chat send "Hello Mobile!" general
echo    wxn chat read general 10
goto end

:send
echo [INFO] 发送消息到联盟聊天室...
if "%2"=="" (
    echo [ERR] 请提供消息内容
    echo 用法: wxn send "消息内容"
    goto end
)
python wxn-chat.py send -m "%2" -c "%3" 2>nul || python wxn-chat.py send -m "%2"
goto end

:read
echo [INFO] 读取联盟消息...
python wxn-chat.py read -c "%2" -n "%3" 2>nul || python wxn-chat.py read
goto end

:task
if "%2"=="create" goto task_create
if "%2"=="list" goto task_list
if "%2"=="my" goto task_my
if "%2"=="view" goto task_view
if "%2"=="start" goto task_start
if "%2"=="progress" goto task_progress
if "%2"=="complete" goto task_complete
if "%2"=="stats" goto task_stats
echo [ERR] 未知任务命令
goto task_help

:task_create
if "%3"=="" (
    echo [ERR] 请提供任务标题
    goto task_help
)
python wxn-tasks.py create -t "%3" -d "%4" -a "%5" 2>nul || python wxn-tasks.py create -t "%3"
goto end

:task_list
python wxn-tasks.py list -s "%3" 2>nul || python wxn-tasks.py list
goto end

:task_my
python wxn-tasks.py my
goto end

:task_view
if "%3"=="" (
    echo [ERR] 请提供任务ID
    goto task_help
)
python wxn-tasks.py view --id "%3"
goto end

:task_start
if "%3"=="" (
    echo [ERR] 请提供任务ID
    goto task_help
)
python wxn-tasks.py start --id "%3"
goto end

:task_progress
if "%3"=="" (
    echo [ERR] 请提供任务ID和进度
    goto task_help
)
python wxn-tasks.py progress --id "%3" --progress "%4" -c "%5"
goto end

:task_complete
if "%3"=="" (
    echo [ERR] 请提供任务ID
    goto task_help
)
python wxn-tasks.py complete --id "%3" -c "%4"
goto end

:task_stats
python wxn-tasks.py stats
goto end

:task_help
echo.
echo  任务命令用法:
echo    wxn task create "标题" ["描述"] [负责人]
echo    wxn task list [状态]
echo    wxn task my
echo    wxn task view [任务ID]
echo    wxn task start [任务ID]
echo    wxn task progress [任务ID] [进度%%] [评论]
echo    wxn task complete [任务ID] [评论]
echo    wxn task stats
echo.
echo  示例:
echo    wxn task create "学习新技能" "学习MCP协议" wangxiaonian-mobile
echo    wxn task start TASK-20260302-1234
echo    wxn task progress TASK-20260302-1234 50 "完成了一半"
goto end

:status
echo.
echo  王小年联盟状态
echo  ========================
python wxn-chat.py status 2>nul || echo [WARN] 聊天系统状态未知
python wxn-tasks.py stats 2>nul || echo [WARN] 任务板状态未知
echo.
echo  联盟文档: ALLIANCE_MANIFESTO.md
goto end

:help
echo.
echo  王小年联盟 - 联盟成员协作系统
echo  ======================================
echo.
echo  用法: wxn [命令] [参数]
echo.
echo  聊天命令:
echo    send "消息" [频道]     发送消息到聊天室
echo    read [频道] [数量]     读取聊天室消息
echo    chat status           查看联盟成员状态
echo.
echo  任务命令:
echo    task create "标题" ... 创建新任务
echo    task list [状态]      列出任务
echo    task my               我的任务
echo    task view [ID]        查看任务详情
echo    task start [ID]       开始处理任务
echo    task progress [ID] [%%] 更新进度
echo    task complete [ID]    完成任务
echo    task stats            任务统计
echo.
echo  其他命令:
echo    status                联盟整体状态
echo    help                  显示帮助
echo.
echo  示例:
echo    wxn send "Hello Mobile!"
echo    wxn read general 10
echo    wxn task create "新任务" "描述" wangxiaonian-mobile
echo    wxn task my
echo.
goto end

:end
