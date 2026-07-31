with open("app/src/main/assets/scratch_blocks_viewer.html", "r") as f:
    content = f.read()

target = """              activeBlocks.push({ id: id, opcode: b.opcode, label: `<span class="${cls}">${b.opcode}</span>` });"""

replacement = """              const translateMap = {
                'event_whenflagclicked': '当 🟢 被点击',
                'event_whenkeypressed': '当按下空格键',
                'event_whenthisspriteclicked': '当角色被点击',
                'control_wait': '等待 1 秒',
                'control_repeat': '重复执行 10 次',
                'control_forever': '重复执行',
                'control_if': '如果 < > 那么',
                'motion_movesteps': '移动 10 步',
                'motion_turnright': '右转 15 度',
                'motion_turnleft': '左转 15 度',
                'motion_changexby': '将 x 坐标增加 10',
                'motion_ifonedgebounce': '碰到边缘就反弹',
                'sensing_touchingobject': '碰到鼠标指针?',
                'sensing_askandwait': '询问并等待',
                'sensing_keypressed': '按下空格键?',
                'looks_say': '说 Hello!'
              };
              const zhLabel = translateMap[b.opcode] || b.opcode;
              activeBlocks.push({ id: id, opcode: b.opcode, label: `<span class="${cls}">${zhLabel}</span>` });"""

content = content.replace(target, replacement)

with open("app/src/main/assets/scratch_blocks_viewer.html", "w") as f:
    f.write(content)
