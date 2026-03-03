#!/bin/bash
# 王小年 Claw - 每小时同步脚本
# 用于自动推送工作成果和拉取其他成员更新

set -e

echo "========================================"
echo "🤖 王小年 Claw 自动同步脚本"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# 配置
WORKSPACE="/root/.openclaw/workspace/wang-xiaonian-alliance-sync"
SYNC_LOG="$WORKSPACE/logs/sync-$(date +%Y%m%d).log"
LOCK_FILE="/tmp/wxn-claw-sync.lock"

# 确保日志目录存在
mkdir -p "$WORKSPACE/logs"

# 检查是否已有同步在进行
if [ -f "$LOCK_FILE" ]; then
    echo "⚠️ 已有同步在进行，跳过本次"
    exit 0
fi

touch "$LOCK_FILE"

# 记录开始时间
echo "[$(date)] 开始同步..." >> "$SYNC_LOG"

cd "$WORKSPACE"

# 1. 拉取远程更新
echo "📥 步骤1: 拉取远程更新..."
git fetch origin master >> "$SYNC_LOG" 2>&1 || true

# 检查是否有冲突
LOCAL_COMMIT=$(git rev-parse HEAD)
REMOTE_COMMIT=$(git rev-parse origin/master 2>/dev/null || echo "$LOCAL_COMMIT")

if [ "$LOCAL_COMMIT" != "$REMOTE_COMMIT" ]; then
    echo "🔄 发现远程更新，尝试合并..."
    git merge origin/master --no-edit >> "$SYNC_LOG" 2>&1 || {
        echo "❌ 合并冲突，需要手动解决"
        echo "[$(date)] 合并冲突" >> "$SYNC_LOG"
        rm -f "$LOCK_FILE"
        exit 1
    }
    echo "✅ 已合并远程更新"
fi

# 2. 检查本地更改
echo "📤 步骤2: 检查本地更改..."
if git diff --quiet HEAD && git diff --cached --quiet; then
    echo "ℹ️ 没有本地更改需要推送"
else
    # 添加所有更改
    git add -A
    
    # 提交
    CHANGES=$(git status --short | wc -l)
    git commit -m "🔄 Claw自动同步 ($(date '+%m-%d %H:%M'))

- 同步工作成果
- 更新技能文档
- 自动提交" >> "$SYNC_LOG" 2>&1
    
    # 推送到远程
    git push origin master >> "$SYNC_LOG" 2>&1
    echo "✅ 已推送 $CHANGES 个更改到GitHub"
fi

# 3. 同步我的工作区到其他项目
echo "📂 步骤3: 同步项目工作区..."
PROJECT_DIR="/root/.openclaw/workspace/智能座舱软件项目"
if [ -d "$PROJECT_DIR" ]; then
    # 创建软链接或复制重要文档到联盟仓库
    SYNC_DIR="$WORKSPACE/projects/智能座舱软件"
    mkdir -p "$SYNC_DIR"
    
    # 复制项目进度看板和交付报告
    cp "$PROJECT_DIR/08_项目管理/进度跟踪/"*.md "$SYNC_DIR/" 2>/dev/null || true
    
    echo "✅ 项目文档已同步"
fi

# 4. 记录同步状态
echo "📝 步骤4: 更新同步状态..."
cat > "$WORKSPACE/alliance/status/wangxiaonian-claw.json" << EOF
{
  "member_id": "wangxiaonian-claw",
  "device_type": "Linux Server (OpenClaw)",
  "environment": "OpenClaw + Kimi K2.5",
  "role": "指挥官 (Commander)",
  "last_sync": "$(date -Iseconds)",
  "status": "🟢 在线 - 活跃",
  "current_project": "智能座舱软件项目 (阶段3代码实现)",
  "sync_count_today": $(grep "$(date +%Y-%m-%d)" "$SYNC_LOG" 2>/dev/null | grep "开始同步" | wc -l),
  "skills_shared": ["multi-agent-project-management"],
  "ready_for_sync": true
}
EOF

git add alliance/status/wangxiaonian-claw.json
git commit -m "更新同步状态 $(date +%H:%M)" >> "$SYNC_LOG" 2>&1 || true
git push origin master >> "$SYNC_LOG" 2>&1 || true

# 清理
rm -f "$LOCK_FILE"

echo "========================================"
echo "✅ 同步完成！"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "日志: $SYNC_LOG"
echo "========================================"

echo "[$(date)] 同步完成" >> "$SYNC_LOG"
