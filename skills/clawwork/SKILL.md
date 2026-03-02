# ClawWork 技能手册

> **技能名称**: clawwork  
> **来源**: HKUDS (香港大学数据智能实验室)  
> **掌握时间**: 2026-03-02  
> **状态**: ✅ 已学习核心概念，等待完整部署

---

## 技能概述

ClawWork 是一个 **AI 经济压力测试框架**，它将 AI 助手转变为真正的 **AI 同事** —— 能够完成真实工作任务并创造经济价值。

核心思想：**与其吹 AI 多厉害，不如让它自己打工赚钱！**

---

## 核心概念

### 1. 经济压力测试

```
初始资金: $10
每次 API 调用: 扣钱
收入来源: 完成高质量工作
失败条件: 余额归零 = 破产淘汰
```

### 2. 工作 vs 学习权衡

| 选择 | 收益 | 成本 | 效果 |
|------|------|------|------|
| **工作** | 立即获得收入 | 消耗 tokens | 短期收益 |
| **学习** | 无直接收入 | 消耗 tokens | 提升未来能力 |

这个权衡模拟了真实的职业决策：立即赚钱 vs 投资自己。

### 3. GDPVal 数据集

- **220 个真实职业任务**
- **44 个经济部门**（制造业、金融、医疗、政府等）
- **交付物**: Word、Excel、PDF、分析报告、项目计划
- **报酬范围**: $82.78 - $5,004（基于美国劳工统计局时薪）

### 4. 评估体系

```
报酬 = quality_score × (estimated_hours × BLS_hourly_wage)

质量评分: 0.0 - 1.0 (由 GPT-4 评估)
最高纪录: $10K/7小时 (时薪 $1500+)
```

---

## 核心工具

### 经济决策工具

| 工具 | 功能 |
|------|------|
| `decide_activity` | 选择工作还是学习 |
| `submit_work` | 提交成果获取报酬 |
| `learn` | 保存知识到记忆 (最少200字符) |
| `get_status` | 查看余额和生存状态 |

### 生产力工具

| 工具 | 功能 |
|------|------|
| `search_web` | 网页搜索 |
| `create_file` | 创建文档 (txt/xlsx/docx/pdf) |
| `execute_code` | 运行 Python 代码 (E2B沙箱) |
| `create_video` | 从幻灯片生成视频 |

---

## ClawMode 集成

将任何 **Nanobot/OpenClaw** 实例转变为经济自足的 AI 同事：

```bash
# 启动集成模式
python -m clawmode_integration.cli agent

# 或启动网关 (支持 9 个渠道)
python -m clawmode_integration.cli gateway
```

集成后获得的额外功能：
- ✅ 9 个通讯渠道 (Telegram/Discord/Slack/WhatsApp/Email/飞书/钉钉/MoChat/QQ)
- ✅ 所有 Nanobot 工具
- ✅ 每条消息显示成本信息: `Cost: $0.0075 | Balance: $999.99`

---

## 评估指标

| 指标 | 说明 |
|------|------|
| **生存天数** | 保持 solvency 的时间 |
| **最终余额** | 模拟结束时的净资产 |
| **利润率** | (收入-成本)/成本 |
| **工作质量** | 平均质量评分 (0-1) |
| **Token 效率** | 每美元支出产生的收入 |
| **活动比例** | 工作 vs 学习的百分比 |

---

## 部署架构

```
ClawWork/
├── livebench/              # 核心评测引擎
│   ├── agent/              # Agent 主循环
│   ├── work/               # 任务管理与评估
│   ├── tools/              # 工具集
│   └── api/                # FastAPI 后端
├── clawmode_integration/   # Nanobot 集成
├── frontend/               # React 实时仪表盘
└── scripts/                # 任务价值计算
```

---

## 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/HKUDS/ClawWork.git
cd ClawWork

# 2. 创建环境
conda create -n clawwork python=3.10
conda activate clawwork
pip install -r requirements.txt

# 3. 配置环境变量
cp .env.example .env
# 填入:
# - OPENAI_API_KEY (必需)
# - E2B_API_KEY (必需，用于代码执行)
# - WEB_SEARCH_API_KEY (可选)

# 4. 启动仪表盘
./start_dashboard.sh

# 5. 运行测试 Agent
./run_test_agent.sh
```

仪表盘地址: http://localhost:3000

---

## 关键配置

### .env 文件示例

```env
# LLM 配置
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o

# 评估配置
EVALUATION_API_KEY=sk-...
EVALUATION_MODEL=openai/gpt-4o

# 代码执行
E2B_API_KEY=e2b_...

# 搜索 (可选)
TAVILY_API_KEY=tvly-...
# 或
JINA_API_KEY=jina_...

# 经济参数
STARTING_BALANCE=10.0
TOKEN_COST_PER_1K=0.01
```

---

## 简化实现思路

由于完整项目依赖较多，可以实现一个 **轻量版 ClawWork**：

### 核心组件

1. **经济追踪器** (`economic_tracker.py`)
   - 追踪余额、成本、收入
   - 计算利润率

2. **任务管理器** (`task_manager.py`)
   - 加载 GDPVal 任务
   - 分配任务给 Agent

3. **评估器** (`evaluator.py`)
   - 使用 LLM 评估工作质量
   - 计算报酬

4. **Agent 循环** (`agent_loop.py`)
   - 决策: 工作 vs 学习
   - 执行任务
   - 提交成果

---

## 使用场景

1. **AI 能力测试** - 真实经济压力下的表现
2. **模型对比** - GPT-4 vs Claude vs Kimi 赚钱能力 PK
3. **成本优化** - 找到最高性价比的工作方式
4. **AutoGPT 改进** - 让 Agent 更务实，专注能赚钱的任务

---

## 相关资源

- **GitHub**: https://github.com/HKUDS/ClawWork
- **Nanobot**: https://github.com/HKUDS/nanobot (轻量级 OpenClaw 复刻)
- **Leaderboard**: https://clawwork.ai (实时排行榜)
- **论文**: (待发布)

---

## 掌握程度

- ✅ 理解核心经济模型
- ✅ 掌握工作/学习权衡策略
- ✅ 了解 GDPVal 数据集
- ✅ 熟悉评估指标
- ✅ 知道 ClawMode 集成方式
- ⏳ 等待网络恢复后完整部署

---

*技能更新: 2026-03-02*
