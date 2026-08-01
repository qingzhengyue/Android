import re

def generate_prefix(grade, className, fallbackId):
    gradeMatch = re.search(r"([一二三四五六七八九十0-9]+)年级", grade)
    if not gradeMatch:
        gradeMatch = re.search(r"([高初][一二三])", grade)
        
    classMatch = re.search(r"([一二三四五六七八九十0-9]+)\s*班", className)
    if not classMatch:
        classMatch = re.search(r"(?<=[(（])[一二三四五六七八九十0-9]+(?=[)）])", className)
    if not classMatch:
        matches = re.findall(r"([一二三四五六七八九十0-9]+)", className)
        classMatch = matches[-1] if matches else None
        
    numMap = {"一": "1", "二": "2", "三": "3", "四": "4", "五": "5", "六": "6", "七": "7", "八": "8", "九": "9", "十": "10", "初一": "7", "初二": "8", "初三": "9", "高一": "10", "高二": "11", "高三": "12"}
    
    gStr = str(fallbackId)
    if gradeMatch:
        g = gradeMatch.group(1) if hasattr(gradeMatch, 'group') else gradeMatch
        gStr = numMap.get(g, g)
        
    cStr = ""
    if classMatch:
        c = classMatch.group(1) if hasattr(classMatch, 'group') else classMatch
        cStr = numMap.get(c, c)
    else:
        cStr = "1"
        
    return gStr + cStr

print("Prefix:", generate_prefix("三年级", "三年级二班", 3))
