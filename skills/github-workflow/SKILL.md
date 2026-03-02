# 🔧 GitHub 工作流程

> Git 仓库创建、SSH 配置与代码推送的标准流程

---

## 📋 技能概览

| 项目 | 说明 |
|------|------|
| **适用场景** | 新项目初始化、代码备份、多端同步 |
| **前提条件** | 已安装 git，有 GitHub 账号 |
| **预计耗时** | 5-10 分钟 |

---

## 🚀 快速开始

### 1. 初始化 Git 仓库

```bash
cd /path/to/your/project
git init
git config user.name "Your Name"
git config user.email "your@email.com"
```

### 2. 生成 SSH 密钥（首次使用）

```bash
# 生成密钥
ssh-keygen -t ed25519 -C "your@email.com" -f ~/.ssh/id_ed25519

# 查看公钥
cat ~/.ssh/id_ed25519.pub
```

### 3. 添加 SSH 密钥到 GitHub

1. 打开 https://github.com/settings/keys
2. 点击 **"New SSH key"**
3. Title 填写设备名称（如：手机王小年）
4. Key 粘贴上方公钥内容
5. 点击 **"Add SSH key"**

### 4. 关联远程仓库

```bash
# 添加远程仓库（SSH 方式）
git remote add origin git@github.com:USERNAME/REPO.git

# 验证
git remote -v
```

### 5. 提交并推送

```bash
# 添加文件
git add .

# 提交
git commit -m "🎉 初始提交"

# 推送
git push -u origin main
```

---

## 📝 常用命令速查

| 操作 | 命令 |
|------|------|
| 查看状态 | `git status` |
| 查看日志 | `git log --oneline` |
| 添加文件 | `git add FILENAME` |
| 提交更改 | `git commit -m "message"` |
| 推送代码 | `git push origin main` |
| 拉取代码 | `git pull origin main` |
| 查看分支 | `git branch` |

---

## 🔍 故障排查

### 推送失败：Permission denied

**原因**：SSH 密钥未配置或配置错误  
**解决**：
```bash
# 测试 SSH 连接
ssh -T git@github.com

# 如失败，检查密钥权限
chmod 600 ~/.ssh/id_ed25519
chmod 644 ~/.ssh/id_ed25519.pub
```

### 推送失败：Repository not found

**原因**：远程仓库 URL 错误或仓库不存在  
**解决**：
```bash
# 检查远程 URL
git remote -v

# 更正 URL
git remote set-url origin git@github.com:USERNAME/REPO.git
```

### 提交失败：Please tell me who you are

**原因**：未配置用户名和邮箱  
**解决**：
```bash
git config user.name "Your Name"
git config user.email "your@email.com"
```

---

## 💡 最佳实践

- ✅ 使用 SSH 而非 HTTPS，避免重复输入密码
- ✅ 提交信息使用 emoji 前缀，增加可读性
  - `🎉` 初始提交
  - `✨` 新功能
  - `🔧` 配置/工具
  - `🐛` 修复 bug
  - `📝` 文档更新
- ✅ 定期 `git pull` 保持代码同步
- ✅ 使用 `.gitignore` 忽略敏感文件和大文件

---

## 🔗 相关资源

- [GitHub SSH 文档](https://docs.github.com/cn/authentication/connecting-to-github-with-ssh)
- [Git 官方文档](https://git-scm.com/doc)

---

*创建时间：2026-03-02*  
*创建者：手机王小年*
