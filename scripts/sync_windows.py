#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kimi 双分身同步系统 - Windows 端
用于 Windows PC 和 Android Termux 之间的同步
"""

import os
import sys
import json
import shutil
import subprocess
import configparser
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional

class KimiSyncWindows:
    """Windows 端同步管理器"""
    
    def __init__(self):
        self.workspace = Path(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        self.sync_dir = self.workspace / "sync"
        self.config_file = self.sync_dir / "sync.conf"
        self.local_skills = self.workspace / "skills"
        self.local_tools = self.workspace / "tools"
        self.local_memory = self.workspace / "memory.md"
        
        self.shared_skills = self.sync_dir / "shared" / "skills"
        self.shared_tools = self.sync_dir / "shared" / "tools"
        self.shared_memory = self.sync_dir / "shared" / "memory"
        
        self.config = self._load_config()
        self.device_id = self.config.get('SYNC', 'device_id', fallback='windows-pc')
        
    def _load_config(self) -> configparser.ConfigParser:
        """加载配置文件"""
        config = configparser.ConfigParser()
        if self.config_file.exists():
            config.read(self.config_file, encoding='utf-8')
        return config
    
    def _log(self, message: str, level: str = "INFO"):
        """记录日志"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_msg = f"[{timestamp}] [{level}] {message}"
        print(log_msg)
        
        # 写入日志文件
        log_file = self.sync_dir / "sync.log"
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(log_msg + '\n')
    
    def _run_git(self, args: List[str], cwd: Optional[Path] = None) -> tuple:
        """运行 git 命令"""
        try:
            result = subprocess.run(
                ["git"] + args,
                cwd=cwd or self.sync_dir,
                capture_output=True,
                text=True,
                encoding='utf-8'
            )
            return result.returncode == 0, result.stdout, result.stderr
        except Exception as e:
            return False, "", str(e)
    
    def init_repo(self, repo_url: str):
        """初始化同步仓库"""
        self._log("初始化同步仓库...")
        
        if (self.sync_dir / ".git").exists():
            self._log("仓库已存在，跳过初始化")
            return True
        
        # 初始化 git 仓库
        success, stdout, stderr = self._run_git(["init"])
        if not success:
            self._log(f"初始化失败: {stderr}", "ERROR")
            return False
        
        # 添加远程仓库
        success, stdout, stderr = self._run_git(["remote", "add", "origin", repo_url])
        if not success and "already exists" not in stderr:
            self._log(f"添加远程仓库失败: {stderr}", "ERROR")
            return False
        
        # 创建初始提交
        self._run_git(["add", "."])
        self._run_git(["commit", "-m", f"Initial sync from {self.device_id}"])
        
        self._log("仓库初始化完成")
        return True
    
    def sync_to_shared(self):
        """将本地内容同步到共享目录"""
        self._log("同步本地内容到共享目录...")
        
        # 同步 skills
        if self.config.getboolean('SYNC_ITEMS', 'skills', fallback=True):
            self._sync_directory(self.local_skills, self.shared_skills)
        
        # 同步 tools
        if self.config.getboolean('SYNC_ITEMS', 'tools', fallback=True):
            self._sync_directory(self.local_tools, self.shared_tools)
        
        # 同步 memory
        if self.config.getboolean('SYNC_ITEMS', 'memory', fallback=True):
            self._sync_memory_to_shared()
        
        self._log("本地同步完成")
    
    def _sync_directory(self, src: Path, dst: Path):
        """同步目录内容"""
        if not src.exists():
            return
        
        dst.mkdir(parents=True, exist_ok=True)
        
        # 获取排除模式
        exclude_patterns = self.config.get('EXCLUDE', 'patterns', fallback='').strip().split('\n')
        exclude_patterns = [p.strip() for p in exclude_patterns if p.strip()]
        
        for item in src.iterdir():
            # 检查排除模式
            if any(self._match_pattern(item.name, pattern) for pattern in exclude_patterns):
                continue
            
            dst_item = dst / item.name
            
            if item.is_dir():
                if dst_item.exists() and not dst_item.is_dir():
                    dst_item.unlink()
                self._sync_directory(item, dst_item)
            else:
                # 复制文件（如果较新或不同）
                if not dst_item.exists() or item.stat().st_mtime > dst_item.stat().st_mtime:
                    shutil.copy2(item, dst_item)
                    self._log(f"同步: {item.name}")
    
    def _match_pattern(self, name: str, pattern: str) -> bool:
        """匹配排除模式"""
        import fnmatch
        return fnmatch.fnmatch(name, pattern)
    
    def _sync_memory_to_shared(self):
        """同步 memory.md 到共享目录"""
        if not self.local_memory.exists():
            return
        
        # 创建设备特定的 memory 文件
        device_memory = self.shared_memory / f"memory_{self.device_id}.md"
        self.shared_memory.mkdir(parents=True, exist_ok=True)
        
        # 复制并添加设备标识
        content = self.local_memory.read_text(encoding='utf-8')
        
        # 检查是否已包含设备标识
        if f"<!-- device: {self.device_id} -->" not in content:
            content = f"<!-- device: {self.device_id} -->\n<!-- synced: {datetime.now().isoformat()} -->\n\n" + content
        
        device_memory.write_text(content, encoding='utf-8')
        self._log(f"Memory 已同步到: {device_memory}")
    
    def sync_from_shared(self):
        """从共享目录同步到本地"""
        self._log("从共享目录同步到本地...")
        
        # 同步 skills
        if self.config.getboolean('SYNC_ITEMS', 'skills', fallback=True):
            self._sync_skills_from_shared()
        
        # 同步 tools
        if self.config.getboolean('SYNC_ITEMS', 'tools', fallback=True):
            self._sync_tools_from_shared()
        
        # 同步 memory
        if self.config.getboolean('SYNC_ITEMS', 'memory', fallback=True):
            self._sync_memory_from_shared()
        
        self._log("远程同步完成")
    
    def _sync_skills_from_shared(self):
        """从共享目录同步 skills"""
        if not self.shared_skills.exists():
            return
        
        strategy = self.config.get('MERGE_STRATEGY', 'skills_strategy', fallback='newest')
        
        for skill_dir in self.shared_skills.iterdir():
            if not skill_dir.is_dir():
                continue
            
            local_skill = self.local_skills / skill_dir.name
            
            # 如果本地不存在，直接复制
            if not local_skill.exists():
                shutil.copytree(skill_dir, local_skill)
                self._log(f"新增 skill: {skill_dir.name}")
                continue
            
            # 根据策略处理冲突
            if strategy == 'newest':
                # 比较修改时间
                shared_mtime = self._get_dir_mtime(skill_dir)
                local_mtime = self._get_dir_mtime(local_skill)
                
                if shared_mtime > local_mtime:
                    shutil.rmtree(local_skill)
                    shutil.copytree(skill_dir, local_skill)
                    self._log(f"更新 skill: {skill_dir.name}")
    
    def _get_dir_mtime(self, path: Path) -> float:
        """获取目录的最新修改时间"""
        mtime = 0
        for item in path.rglob('*'):
            if item.is_file():
                mtime = max(mtime, item.stat().st_mtime)
        return mtime
    
    def _sync_tools_from_shared(self):
        """从共享目录同步 tools"""
        if not self.shared_tools.exists():
            return
        
        # 只同步跨平台兼容的工具
        for tool_file in self.shared_tools.iterdir():
            if tool_file.suffix in ['.py', '.md', '.json', '.yaml', '.yml']:
                local_tool = self.local_tools / tool_file.name
                
                if not local_tool.exists() or tool_file.stat().st_mtime > local_tool.stat().st_mtime:
                    shutil.copy2(tool_file, local_tool)
                    self._log(f"同步 tool: {tool_file.name}")
    
    def _sync_memory_from_shared(self):
        """从共享目录合并 memory"""
        if not self.shared_memory.exists():
            return
        
        # 读取所有设备的 memory
        device_memories = {}
        for memory_file in self.shared_memory.glob("memory_*.md"):
            device_id = memory_file.stem.replace("memory_", "")
            if device_id != self.device_id:
                device_memories[device_id] = memory_file.read_text(encoding='utf-8')
        
        if not device_memories:
            return
        
        # 合并到本地 memory.md
        self._merge_memory(device_memories)
    
    def _merge_memory(self, device_memories: Dict[str, str]):
        """合并多设备的 memory"""
        self._log("合并来自其他设备的记忆...")
        
        local_content = self.local_memory.read_text(encoding='utf-8') if self.local_memory.exists() else ""
        
        # 添加其他设备的更新
        for device_id, content in device_memories.items():
            # 检查是否已包含
            marker = f"<!-- from: {device_id} -->"
            if marker in local_content:
                continue
            
            # 提取关键更新
            lines = content.split('\n')
            new_section = []
            capture = False
            for line in lines:
                if line.startswith('# '):
                    capture = True
                if capture:
                    new_section.append(line)
            
            if new_section:
                local_content += f"\n\n{marker}\n<!-- synced: {datetime.now().isoformat()} -->\n"
                local_content += '\n'.join(new_section[:50])  # 只取前50行
        
        self.local_memory.write_text(local_content, encoding='utf-8')
        self._log(f"已合并 {len(device_memories)} 个设备的记忆")
    
    def push_to_remote(self):
        """推送到远程仓库"""
        self._log("推送到远程仓库...")
        
        # 添加所有变更
        self._run_git(["add", "."])
        
        # 检查是否有变更
        success, stdout, stderr = self._run_git(["status", "--porcelain"])
        if not stdout.strip():
            self._log("没有变更需要推送")
            return True
        
        # 提交
        commit_msg = f"Sync from {self.device_id} at {datetime.now().strftime('%Y-%m-%d %H:%M')}"
        success, stdout, stderr = self._run_git(["commit", "-m", commit_msg])
        
        # 推送
        success, stdout, stderr = self._run_git(["push", "origin", "main"])
        if not success:
            # 尝试 master 分支
            success, stdout, stderr = self._run_git(["push", "origin", "master"])
        
        if success:
            self._log("推送成功")
        else:
            self._log(f"推送失败: {stderr}", "ERROR")
        
        return success
    
    def pull_from_remote(self):
        """从远程仓库拉取"""
        self._log("从远程仓库拉取...")
        
        # 尝试拉取
        success, stdout, stderr = self._run_git(["pull", "origin", "main"])
        if not success:
            success, stdout, stderr = self._run_git(["pull", "origin", "master"])
        
        if success:
            self._log("拉取成功")
        else:
            self._log(f"拉取失败: {stderr}", "WARNING")
        
        return success
    
    def full_sync(self):
        """执行完整同步"""
        self._log("=" * 50)
        self._log("开始完整同步...")
        
        # 1. 拉取远程变更
        self.pull_from_remote()
        
        # 2. 同步到共享目录
        self.sync_to_shared()
        
        # 3. 推送到远程
        self.push_to_remote()
        
        # 4. 从共享目录同步到本地
        self.sync_from_shared()
        
        self._log("同步完成")
        self._log("=" * 50)
        
        # 发送通知
        if self.config.getboolean('NOTIFICATION', 'enabled', fallback=True):
            self._send_notification("同步完成", f"{self.device_id} 同步成功")
    
    def _send_notification(self, title: str, message: str):
        """发送系统通知"""
        try:
            notify_script = self.workspace / "tools" / "windows-auto" / "win_notify.py"
            if notify_script.exists():
                subprocess.run([
                    sys.executable, str(notify_script),
                    "-t", title, "-m", message
                ], capture_output=True)
        except:
            pass
    
    def setup_auto_sync(self):
        """设置自动同步"""
        import sys
        
        # 创建定时任务
        scheduler_script = self.workspace / "tools" / "windows-auto" / "win_scheduler.py"
        if scheduler_script.exists():
            subprocess.run([
                sys.executable, str(scheduler_script),
                "create", "-n", "kimi_sync",
                "-c", f'"{sys.executable}" "{os.path.abspath(__file__)}" auto',
                "--type", "daily",
                "--time", "09:00"
            ])
            self._log("自动同步已设置（每天 9:00）")

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Kimi 双分身同步系统 - Windows")
    parser.add_argument("action", choices=["init", "sync", "auto", "setup"])
    parser.add_argument("--repo", help="GitHub 仓库地址")
    
    args = parser.parse_args()
    
    sync = KimiSyncWindows()
    
    if args.action == "init":
        if not args.repo:
            print("请提供仓库地址: --repo https://github.com/username/kimi-sync.git")
            return
        sync.init_repo(args.repo)
    elif args.action == "sync":
        sync.full_sync()
    elif args.action == "auto":
        sync.full_sync()
    elif args.action == "setup":
        sync.setup_auto_sync()

if __name__ == "__main__":
    main()
