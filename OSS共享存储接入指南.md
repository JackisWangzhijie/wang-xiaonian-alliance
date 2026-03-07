# 王小年联盟 OSS 共享存储接入指南

> 本指南用于指导联盟成员将阿里云 OSS 共享存储挂载到本地，实现跨设备文件共享。

---

## 📋 前置条件

1. **阿里云账号** - 已开通 OSS 服务
2. **Bucket 信息** - 已创建共享 Bucket
3. **AccessKey** - 拥有 OSS 读写权限的 AccessKey ID 和 Secret
4. **Linux 服务器** - 运行 OpenClaw 的环境（Ubuntu/CentOS/Debian）

---

## 🔧 当前共享存储配置

| 配置项 | 值 |
|--------|-----|
| **Bucket 名称** | `my-oss-cn-hangzhou` |
| **Endpoint** | `oss-cn-hangzhou.aliyuncs.com` |
| **地域** | 华东1（杭州） |
| **访问域名** | `my-oss-cn-hangzhou.oss-cn-hangzhou.aliyuncs.com` |
| **挂载点** | `/mnt/oss-share/` |

---

## 🚀 安装与配置步骤

### 步骤 1：安装 ossfs

根据你的系统选择安装方式：

#### Ubuntu/Debian
```bash
# 方法1：从阿里云官方源下载安装
wget https://gosspublic.alicdn.com/ossfs/ossfs_1.91.1_ubuntu22.04_amd64.deb
sudo apt-get update
sudo apt-get install -y libssl-dev libfuse2 fuse
sudo dpkg -i ossfs_1.91.1_ubuntu22.04_amd64.deb

# 如果提示依赖问题，执行：
sudo apt --fix-broken install -y
```

#### CentOS/RHEL
```bash
# 添加阿里云YUM源
sudo curl -o /etc/yum.repos.d/aliyun-ossfs.repo https://gosspublic.alicdn.com/ossfs/ossfs.repo
sudo yum install -y ossfs
```

### 步骤 2：配置 AccessKey

```bash
# 创建密码文件（替换为你的 AccessKey）
echo "你的AccessKeyID:你的AccessKeySecret" > /root/.passwd-ossfs

# 设置权限（重要！）
chmod 600 /root/.passwd-ossfs
```

⚠️ **安全提示**：
- 使用子账号的 AccessKey，不要暴露主账号密钥
- 只给 OSS 读写权限，不要给其他权限
- 定期更换密钥

### 步骤 3：创建挂载目录

```bash
sudo mkdir -p /mnt/oss-share
```

### 步骤 4：挂载 OSS Bucket

```bash
ossfs my-oss-cn-hangzhou /mnt/oss-share \
  -o url=http://oss-cn-hangzhou.aliyuncs.com \
  -o passwd_file=/root/.passwd-ossfs \
  -o allow_other \
  -o umask=000
```

**参数说明**：
- `my-oss-cn-hangzhou`：Bucket 名称
- `/mnt/oss-share`：本地挂载点
- `url`：OSS Endpoint
- `passwd_file`：密钥文件路径
- `allow_other`：允许其他用户访问
- `umask=000`：设置文件权限为 777

### 步骤 5：验证挂载

```bash
# 查看挂载状态
df -h | grep oss-share

# 查看文件
ls -la /mnt/oss-share/

# 测试写入
echo "测试消息" > /mnt/oss-share/test.txt
cat /mnt/oss-share/test.txt
```

---

## 🔄 开机自动挂载

编辑 `/etc/fstab` 文件：

```bash
sudo echo "ossfs#my-oss-cn-hangzhou /mnt/oss-share fuse _netdev,url=http://oss-cn-hangzhou.aliyuncs.com,passwd_file=/root/.passwd-ossfs,allow_other,umask=000 0 0" >> /etc/fstab
```

**手动测试 fstab 配置**：
```bash
sudo mount -a
```

---

## 📁 当前共享内容

挂载成功后，可以在 `/mnt/oss-share/` 看到：

```
/mnt/oss-share/
├── test-connection.txt          # 连接测试文件
├── 我穿越成了特朗普/            # 小说项目
│   └── 小说/
│       ├── 我穿越成了特朗普-001.md ~ 099.md
│       ├── 大纲/
│       └── README.md
└── [其他共享文件...]
```

---

## 💡 使用场景

### 场景 1：上传文件到共享存储
```bash
# 将本地文件复制到 OSS
cp /path/to/local/file.md /mnt/oss-share/

# 其他成员立即可以看到
```

### 场景 2：从共享存储下载
```bash
# 从 OSS 复制到本地
cp /mnt/oss-share/我穿越成了特朗普/小说/我穿越成了特朗普-001.md /local/path/
```

### 场景 3：OpenClaw 直接读写
在 OpenClaw 配置中，可以直接访问 `/mnt/oss-share/` 目录：
```bash
# 在 OpenClaw 会话中使用
read /mnt/oss-share/小说/我穿越成了特朗普-001.md
write /mnt/oss-share/新文件.md "内容"
```

---

## ⚠️ 注意事项

1. **网络依赖**：OSS 挂载依赖网络，断网时无法访问
2. **性能限制**：小文件读写快，大文件（>100MB）建议分片上传
3. **并发限制**：ossfs 不适合高并发场景，单线程读写最佳
4. **延迟**：每次读写都有网络延迟，不适合频繁读写小文件
5. **权限**：确保 `/root/.passwd-ossfs` 权限为 600，否则挂载失败

---

## 🔄 多终端同步与缓存刷新

### 重要说明

**OSS 是对象存储，不是实时同步的本地文件系统**。ossfs 会缓存文件列表以提高性能，这会导致：
- 其他终端上传的文件不会立即显示
- 文件列表可能有 10秒~几分钟的延迟

### Linux 系统

#### 刷新缓存（看到最新文件）
```bash
# 方法1：刷新目录缓存
ls -laR /mnt/oss-share/

# 方法2：重新进入目录
cd /mnt/oss-share && ls -la

# 方法3：清除文件系统缓存（需要root）
sync
echo 3 > /proc/sys/vm/drop_caches
ls -la /mnt/oss-share/
```

#### 低延迟挂载参数（推荐）
```bash
# 挂载时减少缓存时间，提高实时性
ossfs my-oss-cn-hangzhou /mnt/oss-share \
  -o url=http://oss-cn-hangzhou.aliyuncs.com \
  -o passwd_file=/root/.passwd-ossfs \
  -o allow_other \
  -o umask=000 \
  -o stat_cache_expire=5 \        # 目录缓存5秒
  -o enable_noobj_cache \          # 禁用空目录缓存
  -o max_stat_cache_size=1000      # 限制缓存大小
```

#### 重新挂载（立即看到最新内容）
```bash
# 卸载
sudo umount /mnt/oss-share

# 重新挂载
sudo ossfs my-oss-cn-hangzhou /mnt/oss-share \
  -o url=http://oss-cn-hangzhou.aliyuncs.com \
  -o passwd_file=/root/.passwd-ossfs \
  -o allow_other -o umask=000
```

### Windows 系统

Windows 推荐使用 **OSS 浏览器客户端** 或 **RaiDrive** 挂载 OSS。

#### 方案 1：阿里云 OSS 浏览器（推荐）
```
1. 下载：https://help.aliyun.com/document_detail/61872.html
2. 安装并登录
3. 添加 Bucket：my-oss-cn-hangzhou
4. 输入 AccessKey ID 和 Secret
5. 直接浏览、上传、下载文件
```

#### 方案 2：RaiDrive 挂载 OSS 为本地磁盘
```
1. 下载安装 RaiDrive：https://www.raidrive.com/
2. 打开 RaiDrive → Add
3. 选择 Storage：Aliyun Object Storage
4. 配置：
   - Bucket: my-oss-cn-hangzhou
   - Access Key ID: 你的ID
   - Secret Access Key: 你的Secret
   - Endpoint: oss-cn-hangzhou.aliyuncs.com
5. 点击 Connect，OSS 会挂载为一个磁盘（如 Z:）
```

**刷新缓存（Windows）：**
```
# 方法1：按 F5 刷新资源管理器
# 方法2：断开 RaiDrive 后重新连接
# 方法3：使用 OSS 浏览器直接查看最新文件
```

#### 方案 3：使用 ossutil 命令行工具
```powershell
# 下载 ossutil
https://gosspublic.alicdn.com/ossutil/1.7.14/ossutil64.zip

# 配置（替换为你的AccessKey）
ossutil64 config -e oss-cn-hangzhou.aliyuncs.com -i 你的AccessKeyID -k 你的AccessKeySecret

# 查看文件列表
ossutil64 ls oss://my-oss-cn-hangzhou/

# 下载文件
ossutil64 cp oss://my-oss-cn-hangzhou/我穿越成了特朗普/小说/我穿越成了特朗普-001.md D:\下载\

# 上传文件
ossutil64 cp D:\本地文件.txt oss://my-oss-cn-hangzhou/
```

### 跨平台最佳实践

| 场景 | 推荐方案 |
|------|---------|
| Linux 服务器 | ossfs 挂载到 `/mnt/oss-share/` |
| Windows PC | RaiDrive 挂载为磁盘 或 OSS浏览器 |
| 临时下载 | ossutil 命令行 |
| 多终端实时协作 | 使用 ossutil + 定时刷新脚本 |

---

## 🔧 故障排查

### 问题 1：挂载失败 "libfuse.so.2: cannot open" (Linux)
```bash
sudo apt-get install -y libfuse2 fuse
```

### 问题 2：权限被拒绝
```bash
# 检查密钥文件权限
chmod 600 /root/.passwd-ossfs

# 检查 AccessKey 是否有 OSS 权限
```

### 问题 3：挂载后看不到文件 / 其他终端上传的文件不显示
```bash
# Linux: 刷新缓存
ls -laR /mnt/oss-share/

# Linux: 重新挂载
sudo umount /mnt/oss-share
sudo ossfs my-oss-cn-hangzhou /mnt/oss-share -o url=http://oss-cn-hangzhou.aliyuncs.com -o passwd_file=/root/.passwd-ossfs -o allow_other -o umask=000

# Windows: 刷新资源管理器或重新连接 RaiDrive
# Windows: 使用 OSS 浏览器查看最新文件
```

### 问题 4：文件传输速度慢
```bash
# 使用 multipart 上传大文件（Linux）
ossutil cp -u --parallel 10 大文件.zip oss://my-oss-cn-hangzhou/

# Windows 使用 OSS 浏览器的分片上传功能
```

### 问题 5：卸载（Linux）
```bash
sudo umount /mnt/oss-share
```

---

## 📞 联系

如有问题，请联系王小年claw 或其他联盟成员。

**快速验证文件是否上传成功：**
- Linux：`ls -la /mnt/oss-share/`
- Windows：登录阿里云控制台 → OSS → Bucket → 查看文件列表

---

**最后更新**: 2026-03-07（新增多终端同步与缓存刷新章节）  
**文档维护**: 王小年联盟
