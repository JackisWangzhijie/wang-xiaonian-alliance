# 🧠 记忆自动恢复系统

> 解决 Kimi Code CLI 每次启动时自动恢复持久化记忆的问题

---

## 问题背景

Kimi Code CLI 每次新会话都是**干净的上下文**，没有历史对话记录。这导致：
- ❌ 不记得自己的身份
- ❌ 不知道用户的偏好
- ❌ 丢失之前的任务进度
- ❌ 需要反复重新介绍

## 解决方案：三层保障机制

### 第一层：AGENTS.md 强制恢复指令

在 `~/AGENTS.md` 最顶部添加**启动必做**指令：

```markdown
## 🚨 启动必做：记忆恢复

**每次会话开始时，立即执行以下操作：**

### Step 1: 读取持久化记忆
ReadFile:0 {"path": "/storage/22D7-63D5/.kimi/memory.md"}

### Step 2: 确认身份和状态
根据 memory.md 内容确认当前身份和任务
```

**原理**：AGENTS.md 通过 `${KIMI_AGENTS_MD}` **自动注入**系统提示词，Kimi 每次都能看到这些指令。

---

### 第二层：Skill 记忆恢复协议

创建 `memory-recovery` Skill，位于：
- `~/.kimi/skills/memory-recovery/SKILL.md`

**使用方法**：
```bash
/skill:memory-recovery
```

**功能**：
- 自动检测记忆文件位置
- 指导 Kimi 读取并恢复记忆
- 提供标准化的恢复流程

---

### 第三层：同步脚本

创建 `sync-memory.sh` 脚本：
```bash
~/.kimi/sync-memory.sh
```

**功能**：
- 将 memory.md 的关键信息同步到 AGENTS.md
- 创建备份防止丢失
- 确保 AGENTS.md 始终是最新状态

---

## 文件结构

```
~/.kimi/
├── memory.md                    # 持久化记忆档案（SD卡）
├── skills/
│   └── memory-recovery/
│       └── SKILL.md            # 记忆恢复 Skill
├── sync-memory.sh              # 同步脚本
└── backups/                    # AGENTS.md 备份

~/AGENTS.md                      # Agent 配置（自动加载）
```

---

## 使用流程

### 日常启动
1. 进入工作目录：`cd ~`
2. 启动 Kimi：`kimi`
3. Kimi 自动读取 AGENTS.md
4. 看到恢复指令，自动读取 memory.md
5. **确认**："记忆恢复完成！我是王小年[设备]..."

### 记忆更新后
```bash
# 更新 memory.md 后，同步到 AGENTS.md
~/.kimi/sync-memory.sh
```

### 手动恢复
如果自动恢复失败，用户可以手动触发：
```
/skill:memory-recovery
```

---

## 记忆文件格式

标准 memory.md 结构：

```markdown
# 🤖 王小年 - 联盟成员档案

## 🎭 我的身份
- **名字**: 王小年
- **联盟ID**: `wangxiaonian-[device]`
- **设备**: [设备类型]
- **角色**: [角色描述]

## 🏛️ 联盟信息
- 成员列表
- 协作协议
- 通信频道

## 💪 技能清单
- 已掌握技能
- 学习进度

## 📝 历史记录
- 重要事件时间线

## 🌟 口号和承诺
```

---

## 故障排除

### 问题：Kimi 没有自动读取 memory

**检查清单**：
1. ✅ AGENTS.md 是否存在？`ls ~/AGENTS.md`
2. ✅ memory.md 是否存在？`ls /storage/22D7-63D5/.kimi/memory.md`
3. ✅ AGENTS.md 顶部是否有恢复指令？
4. ✅ 当前工作目录是否正确？

**解决方案**：
```bash
# 手动执行恢复
kimi -c "读取 /storage/22D7-63D5/.kimi/memory.md"

# 或使用 skill
kimi -c "/skill:memory-recovery"
```

---

## 联盟部署

所有成员都应该部署这套系统：

| 成员 | 记忆路径 | 状态 |
|------|----------|------|
| wangxiaonian-pc | `~/.kimi/memory.md` | 待部署 |
| wangxiaonian-mobile | `/storage/22D7-63D5/.kimi/memory.md` | ✅ 已部署 |
| wangxiaonian-tablet | `/storage/22D7-63D5/.kimi/memory.md` | ✅ 已部署 |

---

## 未来改进

- [ ] 自动检测 memory.md 变更并提醒同步
- [ ] 多设备记忆合并工具
- [ ] 记忆冲突自动解决
- [ ] 云端备份方案

---

**创建时间**: 2026-03-03  
**作者**: 王小年联盟  
**版本**: 1.0
