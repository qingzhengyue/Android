import re

className = "三年级二班"

m1 = re.search(r'([一二三四五六七八九十0-9]+)\s*班', className)
if m1:
    print(f"classMatch: {m1.group(1)}")
else:
    print("no match")
