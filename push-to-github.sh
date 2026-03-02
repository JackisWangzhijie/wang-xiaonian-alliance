#!/data/data/com.termux/files/usr/bin/sh
# 王小年联盟 - GitHub 推送助手

echo "================================"
echo "🚀 推送到 GitHub"
echo "================================"
echo ""
echo "仓库: wangzhijie163/wang-xiaonian-alliance"
echo ""

cd /data/data/com.termux/files/home/kimi-workspace/wangxiaonian-alliance

# 检查是否有 token 文件
if [ -f ~/.github_token ]; then
    TOKEN=$(cat ~/.github_token)
    echo "[*] 使用保存的 Token 推送..."
    git push https://wangzhijie163:$TOKEN@github.com/wangzhijie163/wang-xiaonian-alliance.git main
else
    echo "请输入 GitHub Token:"
    echo "(输入时不会显示，输入完按回车)"
    read -s TOKEN
    
    if [ -n "$TOKEN" ]; then
        echo "[*] 推送中..."
        git push https://wangzhijie163:$TOKEN@github.com/wangzhijie163/wang-xiaonian-alliance.git main
        
        # 询问是否保存 token
        echo ""
        echo "是否保存 Token 以便下次使用? (y/n)"
        read SAVE
        if [ "$SAVE" = "y" ]; then
            echo "$TOKEN" > ~/.github_token
            chmod 600 ~/.github_token
            echo "✅ Token 已保存到 ~/.github_token"
        fi
    else
        echo "❌ Token 为空，取消推送"
    fi
fi

echo ""
echo "================================"
