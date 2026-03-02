#!/bin/bash
# -*- coding: utf-8 -*-
# Kimi 双分身同步系统 - Termux 端
# 用于 Android Termux 和 Windows PC 之间的同步

set -e

# 配置
WORKSPACE="${HOME}/kimi-workspace"
SYNC_DIR="${WORKSPACE}/sync"
LOCAL_SKILLS="${WORKSPACE}/skills"
LOCAL_TOOLS="${WORKSPACE}/tools"
LOCAL_MEMORY="${WORKSPACE}/memory.md"

SHARED_SKILLS="${SYNC_DIR}/shared/skills"
SHARED_TOOLS="${SYNC_DIR}/shared/tools"
SHARED_MEMORY="${SYNC_DIR}/shared/memory"

CONFIG_FILE="${SYNC_DIR}/sync.conf"
DEVICE_ID="termux-mobile"
LOG_FILE="${SYNC_DIR}/sync.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "[${timestamp}] [${level}] ${message}"
    echo "[${timestamp}] [${level}] ${message}" >> "$LOG_FILE"
}

# 读取配置
read_config() {
    if [ -f "$CONFIG_FILE" ]; then
        # 使用 grep 和 cut 简单解析
        DEVICE_ID=$(grep "^device_id" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
        DEVICE_ID=${DEVICE_ID:-"termux-mobile"}
    fi
}

# 初始化仓库
init_repo() {
    local repo_url="$1"
    log "INFO" "初始化同步仓库..."
    
    if [ -d "${SYNC_DIR}/.git" ]; then
        log "INFO" "仓库已存在"
        return 0
    fi
    
    cd "$SYNC_DIR"
    git init
    git remote add origin "$repo_url" 2>/dev/null || true
    git add .
    git commit -m "Initial sync from ${DEVICE_ID}" || true
    
    log "INFO" "仓库初始化完成"
}

# 同步目录（带排除模式）
sync_directory() {
    local src="$1"
    local dst="$2"
    
    if [ ! -d "$src" ]; then
        return 0
    fi
    
    mkdir -p "$dst"
    
    # 使用 rsync 或 cp -r，排除特定文件
    if command -v rsync &> /dev/null; then
        rsync -av --exclude='*.tmp' --exclude='*.log' --exclude='__pycache__' \
              --exclude='.DS_Store' --exclude='*.pyc' --exclude='.env' \
              "$src/" "$dst/"
    else
        # 使用 cp，手动排除
        find "$src" -type f \
            ! -name '*.tmp' \
            ! -name '*.log' \
            ! -path '*/__pycache__/*' \
            ! -name '.DS_Store' \
            ! -name '*.pyc' \
            ! -name '.env' | while read file; do
            rel_path="${file#$src/}"
            dst_file="${dst}/${rel_path}"
            mkdir -p "$(dirname "$dst_file")"
            
            # 只复制较新的文件
            if [ ! -f "$dst_file" ] || [ "$file" -nt "$dst_file" ]; then
                cp -p "$file" "$dst_file"
                log "INFO" "同步: $(basename "$file")"
            fi
        done
    fi
}

# 同步 memory 到共享目录
sync_memory_to_shared() {
    if [ ! -f "$LOCAL_MEMORY" ]; then
        return 0
    fi
    
    mkdir -p "$SHARED_MEMORY"
    local device_memory="${SHARED_MEMORY}/memory_${DEVICE_ID}.md"
    
    # 添加设备标识
    {
        echo "<!-- device: ${DEVICE_ID} -->"
        echo "<!-- synced: $(date -Iseconds) -->"
        echo ""
        cat "$LOCAL_MEMORY"
    } > "$device_memory"
    
    log "INFO" "Memory 已同步到: ${device_memory}"
}

# 从共享目录同步 skills
sync_skills_from_shared() {
    if [ ! -d "$SHARED_SKILLS" ]; then
        return 0
    fi
    
    for skill_dir in "$SHARED_SKILLS"/*/; do
        [ -d "$skill_dir" ] || continue
        
        local skill_name=$(basename "$skill_dir")
        local local_skill="${LOCAL_SKILLS}/${skill_name}"
        
        # 如果本地不存在，直接复制
        if [ ! -d "$local_skill" ]; then
            cp -r "$skill_dir" "$local_skill"
            log "INFO" "新增 skill: ${skill_name}"
            
            # 发送通知
            if command -v termux-notification &> /dev/null; then
                termux-notification -t "Kimi Sync" -c "新技能: ${skill_name}"
            fi
            continue
        fi
        
        # 比较修改时间，保留较新的
        local shared_mtime=$(find "$skill_dir" -type f -printf '%T@\n' | sort -n | tail -1)
        local local_mtime=$(find "$local_skill" -type f -printf '%T@\n' | sort -n | tail -1)
        
        if (( $(echo "$shared_mtime > $local_mtime" | bc -l) )); then
            rm -rf "$local_skill"
            cp -r "$skill_dir" "$local_skill"
            log "INFO" "更新 skill: ${skill_name}"
            
            # 发送通知
            if command -v termux-notification &> /dev/null; then
                termux-notification -t "Kimi Sync" -c "技能更新: ${skill_name}"
            fi
        fi
    done
}

# 从共享目录同步 tools
sync_tools_from_shared() {
    if [ ! -d "$SHARED_TOOLS" ]; then
        return 0
    fi
    
    for tool_file in "$SHARED_TOOLS"/*.{py,md,json,yaml,yml}; do
        [ -f "$tool_file" ] || continue
        
        local tool_name=$(basename "$tool_file")
        local local_tool="${LOCAL_TOOLS}/${tool_name}"
        
        if [ ! -f "$local_tool" ] || [ "$tool_file" -nt "$local_tool" ]; then
            cp -p "$tool_file" "$local_tool"
            log "INFO" "同步 tool: ${tool_name}"
        fi
    done
}

# 合并 memory
merge_memory() {
    if [ ! -d "$SHARED_MEMORY" ]; then
        return 0
    fi
    
    log "INFO" "合并来自其他设备的记忆..."
    
    local local_content=""
    if [ -f "$LOCAL_MEMORY" ]; then
        local_content=$(cat "$LOCAL_MEMORY")
    fi
    
    local merged_count=0
    
    for memory_file in "$SHARED_MEMORY"/memory_*.md; do
        [ -f "$memory_file" ] || continue
        
        local device_id=$(basename "$memory_file" .md | sed 's/memory_//')
        
        # 跳过自己
        if [ "$device_id" = "$DEVICE_ID" ]; then
            continue
        fi
        
        local marker="<!-- from: ${device_id} -->"
        
        # 检查是否已包含
        if echo "$local_content" | grep -q "$marker"; then
            continue
        fi
        
        # 提取内容并添加标记
        local_content="${local_content}

${marker}
<!-- synced: $(date -Iseconds) -->

$(head -50 "$memory_file")"
        
        merged_count=$((merged_count + 1))
    done
    
    echo "$local_content" > "$LOCAL_MEMORY"
    log "INFO" "已合并 ${merged_count} 个设备的记忆"
}

# 推送到远程
push_to_remote() {
    log "INFO" "推送到远程仓库..."
    
    cd "$SYNC_DIR"
    
    git add .
    
    # 检查是否有变更
    if git diff --cached --quiet; then
        log "INFO" "没有变更需要推送"
        return 0
    fi
    
    local commit_msg="Sync from ${DEVICE_ID} at $(date '+%Y-%m-%d %H:%M')"
    git commit -m "$commit_msg" || true
    
    if git push origin main 2>/dev/null || git push origin master 2>/dev/null; then
        log "INFO" "推送成功"
        
        # 发送通知
        if command -v termux-notification &> /dev/null; then
            termux-notification -t "Kimi Sync" -c "同步成功！"
        fi
    else
        log "ERROR" "推送失败"
    fi
}

# 从远程拉取
pull_from_remote() {
    log "INFO" "从远程仓库拉取..."
    
    cd "$SYNC_DIR"
    
    git fetch origin || true
    
    if git pull origin main 2>/dev/null || git pull origin master 2>/dev/null; then
        log "INFO" "拉取成功"
    else
        log "WARNING" "拉取失败或没有更新"
    fi
}

# 完整同步
full_sync() {
    log "INFO" "========================================"
    log "INFO" "开始完整同步..."
    
    read_config
    
    # 1. 拉取远程变更
    pull_from_remote
    
    # 2. 同步本地到共享
    sync_directory "$LOCAL_SKILLS" "$SHARED_SKILLS"
    sync_directory "$LOCAL_TOOLS" "$SHARED_TOOLS"
    sync_memory_to_shared
    
    # 3. 推送到远程
    push_to_remote
    
    # 4. 从共享同步到本地
    sync_skills_from_shared
    sync_tools_from_shared
    merge_memory
    
    log "INFO" "同步完成"
    log "INFO" "========================================"
}

# 设置自动同步（使用 crontab）
setup_auto_sync() {
    log "INFO" "设置自动同步..."
    
    # 检查 crontab 是否已存在该任务
    if crontab -l 2>/dev/null | grep -q "kimi-sync"; then
        log "INFO" "自动同步已设置"
        return 0
    fi
    
    # 添加定时任务（每 30 分钟）
    (crontab -l 2>/dev/null; echo "*/30 * * * * ${SYNC_DIR}/scripts/sync_termux.sh sync >> ${LOG_FILE} 2>&1") | crontab -
    
    # 启动 crond
    if ! pgrep -x "crond" > /dev/null; then
        crond
    fi
    
    log "INFO" "自动同步已设置（每 30 分钟）"
    
    # 发送通知
    if command -v termux-notification &> /dev/null; then
        termux-notification -t "Kimi Sync" -c "自动同步已启用"
    fi
}

# 主函数
main() {
    local action="${1:-sync}"
    
    # 确保目录存在
    mkdir -p "$SYNC_DIR" "$SHARED_SKILLS" "$SHARED_TOOLS" "$SHARED_MEMORY"
    
    case "$action" in
        init)
            if [ -z "$2" ]; then
                echo "用法: $0 init <repo-url>"
                exit 1
            fi
            init_repo "$2"
            ;;
        sync)
            full_sync
            ;;
        setup)
            setup_auto_sync
            ;;
        *)
            echo "用法: $0 [init|sync|setup]"
            echo "  init <repo-url>  - 初始化同步仓库"
            echo "  sync             - 执行同步"
            echo "  setup            - 设置自动同步"
            exit 1
            ;;
    esac
}

main "$@"
