#!/data/data/com.termux/files/usr/bin/sh
# 王小年联盟 - GitHub 设置脚本

REPO_DIR="/data/data/com.termux/files/home/kimi-workspace/wangxiaonian-alliance"
cd "$REPO_DIR"

echo "================================"
echo "🚀 王小年联盟 GitHub 设置"
echo "================================"
echo ""
echo "步骤1: 在 GitHub 创建私有仓库"
echo "--------------------------------"
echo "访问: https://github.com/new"
echo ""
echo "填写信息:"
echo "  Repository name: wang-xiaonian-alliance"
echo "  Description: 王小年联盟 - AI助手分布式记忆系统"
echo "  Visibility: 🔒 Private (重要!)"
echo "  ✅ Initialize with README"
echo ""
read -p "按回车继续...(创建好后)"

echo ""
echo "步骤2: 配置远程仓库"
echo "--------------------------------"
echo "请输入您的 GitHub 用户名:"
read USERNAME
echo ""

REMOTE_URL="git@github.com:$USERNAME/wang-xiaonian-alliance.git"
HTTPS_URL="https://github.com/$USERNAME/wang-xiaonian-alliance.git"

echo "选择连接方式:"
echo "1) SSH (推荐，需要配置密钥)"
echo "2) HTTPS (需要输入密码)"
read -p "选择 (1/2): " CHOICE

if [ "$CHOICE" = "1" ]; then
    git remote add origin "$REMOTE_URL"
    echo "✅ SSH 远程仓库已添加"
    echo ""
    echo "⚠️  请确保已配置 SSH 密钥:"
    echo "   ssh-keygen -t ed25519 -C 'wangxiaonian@kimi.ai'"
    echo "   cat ~/.ssh/id_ed25519.pub"
    echo "   # 复制到 GitHub Settings -> SSH Keys"
else
    git remote add origin "$HTTPS_URL"
    echo "✅ HTTPS 远程仓库已添加"
fi

echo ""
echo "步骤3: 首次推送"
echo "--------------------------------"
git branch -M main
git push -u origin main 2>/dev/null && echo "✅ 推送成功!" || echo "⚠️ 推送失败，请检查网络或认证"

echo ""
echo "================================"
echo "🎉 王小年联盟已连接 GitHub!"
echo "================================"
echo ""
echo "PC王小年恢复后，可以克隆这个仓库:"
echo "   git clone $HTTPS_URL"
