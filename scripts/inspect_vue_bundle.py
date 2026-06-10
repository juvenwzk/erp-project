import re
p = r"c:\Users\王照康\IdeaProjects\erp-project\erp-server\src\main\resources\static\assets\index-CMpYOR6T.css"
t = open(p, encoding="utf-8", errors="ignore").read()
names = set()
for m in re.findall(r'\.([a-zA-Z0-9_-]+)\{', t):
    if any(k in m for k in ['sidebar', 'header', 'content', 'main', 'page', 'app', 'layout', 'nav']):
        if len(m) < 35:
            names.add(m)
for n in sorted(names):
    print(n)
