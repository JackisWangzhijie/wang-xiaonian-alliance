# 统计并报告当前文件字数
import os
import glob

total_chars = 0
for i in range(76, 91):
    filepath = f"都市爱情故事-{i:03d}.md"
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            chars = len(content)
            total_chars += chars
            print(f"{filepath}: {chars} 字符")
    else:
        print(f"{filepath}: 文件不存在")

print(f"\n总计: {total_chars} 字符")
print(f"约 {total_chars//2} 中文字")
