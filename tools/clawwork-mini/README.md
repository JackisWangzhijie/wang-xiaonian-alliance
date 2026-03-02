# ClawWork Mini 🎯

> 简化版 AI 经济压力测试框架  
> 基于 HKUDS ClawWork 核心概念实现

---

## 简介

ClawWork Mini 是一个**简化版的 AI 经济压力测试框架**，让 AI Agent 在**真实经济压力**下工作：

- 💰 **初始资金**: $10
- 💸 **每次思考**: 消耗 tokens (扣钱)
- 📈 **完成任务**: 获得报酬
- 💀 **余额归零**: 破产淘汰

这个框架帮助理解 **AI 经济可持续性** - 不是能做什么，而是能**赚多少钱**。

---

## 快速开始

```bash
# 1. 进入目录
cd ~/kimi-workspace/tools/clawwork-mini

# 2. 运行模拟
python3 clawwork_agent.py --days 5

# 或自定义 Agent 名称和天数
python3 clawwork_agent.py --name "MyAgent" --days 10

# 重置状态重新开始
python3 clawwork_agent.py --reset
```

---

## 核心概念

### 1. 经济压力测试

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  初始 $10   │────→│  工作/学习  │────→│  赚钱/扣费  │
└─────────────┘     └─────────────┘     └──────┬──────┘
      ↑                                          │
      └──────────────────────────────────────────┘
                   循环直到破产或成功
```

### 2. 工作 vs 学习权衡

| 选择 | 立即收益 | 长期收益 | 风险 |
|------|---------|---------|------|
| **工作** | 💰 获得报酬 | - | 质量不高收入低 |
| **学习** | ❌ 无收入 | 🚀 能力提升 | 消耗资金无回报 |

### 3. 任务系统

基于 **GDPVal** 数据集简化版：
- 8 个行业领域 (金融/制造/医疗/科技等)
- 不同难度和报酬
- 质量评分决定最终收入

---

## 文件结构

```
clawwork-mini/
├── README.md              # 本文档
├── economic_tracker.py    # 经济追踪器
├── task_manager.py        # 任务管理器
├── clawwork_agent.py      # Agent 主程序
└── *.json                 # 状态保存文件
```

---

## 使用示例

### 基本使用

```bash
$ python3 clawwork_agent.py --days 3

🚀 启动 ClawWork Mini 模拟
   Agent: ClawWorker
   初始资金: $10.00
   目标: 生存 3 天

==================================================
📅 第 1 天 - ClawWorker
==================================================

🎯 CLAWWORK MINI - 经济仪表盘
==================================================
💰 当前余额: $10.0
📈 累计收入: $0.0
📉 累计成本: $0.0
💵 净利润: $0.0
📊 利润率: 0.0%
⚡ 效率: 0.00x
✅ 完成任务: 0
⏱️  生存时间: 0.0 hours
🎮 状态: 🟢 活跃
==================================================

🤔 决策: 稳健策略，先赚钱

🎯 开始任务: 市场分析报告
   预期收入: $211.20
💭 思考完成 (消耗 37 tokens)
💭 思考完成 (消耗 37 tokens)
📤 提交成果...
📊 质量评分: 0.85/1.0
💰 获得报酬: $179.52

🎯 CLAWWORK MINI - 经济仪表盘
==================================================
💰 当前余额: $189.52
📈 累计收入: $179.52
📉 累计成本: $0.12
💵 净利润: $179.40
📊 利润率: 149500.0%
⚡ 效率: 1496.00x
✅ 完成任务: 1
🎮 状态: 🟢 活跃
==================================================
```

---

## 评估指标

| 指标 | 说明 |
|------|------|
| **余额** | 当前可用资金 |
| **收入** | 完成任务获得的总报酬 |
| **成本** | Token 消耗总成本 |
| **利润率** | (收入-成本)/成本 |
| **效率** | 每美元成本产生的收入 |
| **任务数** | 完成的任务数量 |

---

## 与完整版 ClawWork 对比

| 功能 | ClawWork Mini | 完整版 ClawWork |
|------|---------------|-----------------|
| 经济模型 | ✅ 简化版 | ✅ 完整版 |
| 任务数量 | 8 个模拟任务 | 220 个真实任务 |
| 评估方式 | 规则基础 | GPT-4 评估 |
| 仪表盘 | 命令行 | React Web |
| LLM 集成 | 模拟 | 真实 API |
| 代码执行 | ❌ | ✅ E2B 沙箱 |
| 多模型支持 | ❌ | ✅ GPT/Claude/Kimi等 |
| ClawMode | ❌ | ✅ Nanobot 集成 |

---

## 进阶使用

### 自定义 Agent

```python
from clawwork_agent import ClawWorkAgent

# 创建 Agent
agent = ClawWorkAgent(name="MyAI")

# 运行单天
agent.run_day()

# 运行多天模拟
agent.run_simulation(days=10)

# 查看状态
print(agent.tracker.get_status())
```

### 自定义任务

```python
from task_manager import TaskManager, Task

manager = TaskManager()

# 添加自定义任务
custom_task = Task(
    id="custom_001",
    title="自定义任务",
    category="technology",
    description="任务描述",
    deliverable="交付物要求",
    estimated_hours=3,
    hourly_wage=50.0,
    max_payment=150.0,
    difficulty=3
)

manager.tasks.append(custom_task)
```

---

## 相关资源

- **ClawWork 原版**: https://github.com/HKUDS/ClawWork
- **Nanobot**: https://github.com/HKUDS/nanobot
- **HKUDS 实验室**: https://github.com/HKUDS

---

## 许可证

MIT License - 仅供学习研究使用

---

*简化版实现: 2026-03-02*  
*学习自: HKUDS ClawWork*
