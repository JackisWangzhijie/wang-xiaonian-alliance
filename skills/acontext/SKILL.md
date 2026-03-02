---
name: acontext
description: Acontext 开源AI上下文管理器 - 解锁Agent无限记忆。用于存储对话和工件、观察任务进度、自我学习积累技能(SOP)。当需要突破上下文限制、实现长期记忆、任务追踪、技能积累时使用。
homepage: https://acontext.io
---

# Acontext - Agent无限记忆系统

Acontext 是一个围绕AI代理工作流程设计的**上下文数据平台**，通过"存储-观察-学习"的闭环，让Agent拥有真正的长期记忆和自我进化能力。

## 核心价值

1. **突破上下文限制** - 外部存储对话和工件，不受LLM上下文窗口限制
2. **任务智能追踪** - 自动提取任务、跟踪进度、记录用户偏好
3. **自我学习进化** - 从完成的任务中提炼标准操作流程(SOP)，越用越聪明
4. **可视化监控** - 本地仪表板实时查看消息、任务、工件和技能

---

## 核心概念

### 1. Session - 对话笔记本
存储用户与代理之间的完整交互历史，支持多模态消息(文本/图片/音频)。

```python
session = client.sessions.create()
# 支持格式: OpenAI, Anthropic 等
client.sessions.send_message(
    session_id=session.id,
    blob={"role": "user", "content": "需求..."},
    format="openai"
)
```

### 2. Task Agent - 任务追踪员
自动从对话中提取任务、跟踪进度、记录用户偏好。

```python
# 自动识别任务并查询状态
tasks = client.sessions.get_tasks(session.id)
for task in tasks.items:
    print(f"任务: {task.data['task_description']}")
    print(f"状态: {task.status}")  # pending/processing/success/failed
    print(f"用户偏好: {task.data.get('user_preferences', [])}")
```

### 3. Disk - 文件柜
存储代理生成的工件(代码/文档/图片)，通过文件路径管理。

```python
disk = client.disks.create()
artifact = client.disks.artifacts.upsert(
    disk.id,
    file=FileUpload(filename="todo.md", content=b"..."),
    file_path="/project/iphone-landing/"
)
```

### 4. Space - 技能知识库
类似Notion的结构化系统，存储从经验中提炼的SOP技能。

SOP块结构:
```json
{
    "use_when": "star a repo on github.com",
    "preferences": "use personal account. star but not fork",
    "tool_sops": [
        {"tool_name": "goto", "action": "goto github.com"},
        {"tool_name": "click", "action": "find login button if any"}
    ]
}
```

### 5. Experience Agent - 技能提炼师
后台自动从完成的任务中提炼SOP，判断复杂度，存储到Space。

---

## 快速开始

### 1. 安装 CLI
```bash
curl -fsSL https://install.acontext.io | sh
```

### 2. 启动服务 (需要 Docker)
```bash
acontext docker up
```
服务地址:
- API: http://localhost:8029/api/v1
- 仪表板: http://localhost:3000

### 3. 安装 SDK
```bash
pip install acontext
```

### 4. 初始化客户端
```python
from acontext import AcontextClient

client = AcontextClient(
    base_url="http://localhost:8029/api/v1",
    api_key="sk-ac-your-root-api-bearer-token"
)
```

---

## 工作流程

```
用户 <-> 你的代理 <-> Session <-> Disk(工件)
                ↓
         观察到的任务(Task Agent)
                ↓
         Space(技能学习) <-> 指导代理
```

1. **存储**: 对话保存到Session，工件保存到Disk
2. **观察**: Task Agent自动提取任务、跟踪状态、记录偏好
3. **学习**: 任务完成后，Experience Agent提炼SOP到Space
4. **应用**: 新任务从Space检索相关技能指导执行

---

## 完整示例

```python
from acontext import AcontextClient, FileUpload
import openai

# 初始化
client = AcontextClient(
    base_url="http://localhost:8029/api/v1",
    api_key="your-api-key"
)

# 创建带Space的Session(启用学习)
space = client.spaces.create()
session = client.sessions.create(space_id=space.id)
disk = client.disks.create()

# 对话并保存
messages = [
    {"role": "user", "content": "帮我搭建一个电商网站"},
    {"role": "assistant", "content": "计划:\n1. 需求分析\n2. 数据库设计\n3. API开发\n4. 前端实现\n5. 部署上线"}
]
for msg in messages:
    client.sessions.send_message(session.id, blob=msg, format="openai")

# 存储工件
code_file = FileUpload(filename="app.py", content=b"# 代码...")
client.disks.artifacts.upsert(disk.id, file=code_file, file_path="/src/")

# 刷新触发任务提取
client.sessions.flush(session.id)

# 查看任务进度
tasks = client.sessions.get_tasks(session.id)
for task in tasks.items:
    print(f"#{task.order}: {task.data['task_description']} [{task.status}]")

# 下次会话检索技能
skills = client.spaces.experience_search(
    space_id=space.id,
    query="部署Next.js到Vercel",
    mode="fast"  # fast 或 agentic
)
```

---

## 一句话总结

> Acontext = 对话存储(Session) + 任务追踪(Task Agent) + 工件管理(Disk) + 技能学习(Space) = Agent的无限记忆系统

---

## 资源链接

- 官网: https://acontext.io
- 文档: https://docs.acontext.io
- GitHub: https://github.com/context-data/acontext
