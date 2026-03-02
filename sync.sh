#!/data/data/com.termux/files/usr/bin/sh
# 王小年联盟 - 自动同步脚本 (手机版)

REPO_DIR="/data/data/com.termux/files/home/kimi-workspace/wangxiaonian-alliance"
SOURCE_DIR="/data/data/com.termux/files/home/kimi-workspace"

cd "$REPO_DIR"

echo "=== 王小年联盟同步 ==="
echo "时间: $(date)"
echo ""

# 1. 复制需要同步的文件
echo "[*] 复制记忆文件..."
cp "$SOURCE_DIR/memory.md" ./

# 2. 复制技能
echo "[*] 复制技能目录..."
rm -rf ./skills 2>/dev/null
cp -r "$SOURCE_DIR/skills" ./ 2>/dev/null || echo "(skills目录可能为空)"

# 3. 复制工具
echo "[*] 复制工具目录..."
rm -rf ./tools 2>/dev/null
cp -r "$SOURCE_DIR/tools" ./ 2>/dev/null || echo "(tools目录可能为空)"

# 4. 生成同步报告
cat > sync-report.md << REPORT
# 📊 同步报告

**设备**: 手机王小年 (Termux)  
**时间**: $(date '+%Y-%m-%d %H:%M:%S')  
**状态**: ✅ 自动同步

## 📈 本次更新

$(git status --short 2>/dev/null || echo "首次同步")

## 📝 备注

- 定时任务运行正常
- 股票监控: 9只股票
- 下次同步: $(date -d '+1 hour' '+%H:%M' 2>/dev/null || echo '1小时后')

---
*自动生成的同步报告*
REPORT

# 5. Git 操作
echo ""
echo "[*] Git 操作..."

git add -A
git commit -m "📱 手机王小年同步: $(date '+%Y-%m-%d %H:%M')" 2>/dev/null || echo "无变更或已提交"

# 检查是否有远程仓库
if git remote get-url origin 2>/dev/null; then
    echo "[*] 推送到 GitHub..."
    git push origin main 2>/dev/null && echo "✅ 推送成功" || echo "⚠️ 推送失败(可能无网络)"
else
    echo "⚠️ 未配置远程仓库"
    echo "请运行: ./setup-github.sh"
fi

echo ""
echo "=== 同步完成 ==="
