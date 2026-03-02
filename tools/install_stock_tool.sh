#!/bin/bash
# A股查询工具安装脚本

echo "🚀 开始安装 A股查询工具..."

# 检查Python
echo "📋 检查Python环境..."
if ! command -v python3 &> /dev/null; then
    echo "❌ 未找到Python3，请先安装Python"
    exit 1
fi

echo "✅ Python3 已安装"

# 安装依赖
echo "📦 安装依赖包 (akshare, pandas)..."
pip install akshare pandas -i https://pypi.tuna.tsinghua.edu.cn/simple

if [ $? -ne 0 ]; then
    echo "❌ 依赖安装失败，请检查网络连接"
    exit 1
fi

echo "✅ 依赖安装完成"

# 创建快捷命令
echo "🔗 创建快捷命令..."
TOOL_PATH="$HOME/kimi-workspace/tools/cn_stock_tool.py"
chmod +x "$TOOL_PATH"

# 添加到.bashrc
if ! grep -q "alias stock=" "$HOME/.bashrc"; then
    echo "" >> "$HOME/.bashrc"
    echo "# A股查询工具快捷命令" >> "$HOME/.bashrc"
    echo "alias stock='python3 $TOOL_PATH'" >> "$HOME/.bashrc"
    echo "✅ 快捷命令 'stock' 已添加到 .bashrc"
    echo "   请运行 'source ~/.bashrc' 或重新打开终端使配置生效"
else
    echo "✅ 快捷命令已存在"
fi

echo ""
echo "🎉 安装完成！"
echo ""
echo "使用方法:"
echo "  stock quote 600519      # 查询茅台实时行情"
echo "  stock gainers           # 查看涨幅榜"
echo "  stock losers            # 查看跌幅榜"
echo "  stock search 茅台       # 搜索股票"
echo "  stock kline 600519 -d 60 # 查询60日K线"
echo ""
echo "或者直接使用:"
echo "  python3 $TOOL_PATH --help"
