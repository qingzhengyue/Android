import json

data = """{"targets":[{"isStage":true,"name":"Stage","variables":{},"lists":{},"broadcasts":{},"blocks":{},"comments":{},"currentCostume":0,"costumes":[{"name":"背景1","bitmapResolution":1,"dataFormat":"svg","assetId":"cd21584322f79459ecb5864133b44723","md5ext":"cd21584322f79459ecb5864133b44723.svg","rotationCenterX":240,"rotationCenterY":180}],"sounds":[],"volume":100,"layerOrder":0},{"isStage":false,"name":"角色1","variables":{},"lists":{},"broadcasts":{},"blocks":{"a":{"opcode":"event_whenflagclicked","next":"b","parent":null,"inputs":{},"fields":{},"shadow":false,"topLevel":true,"x":100,"y":100},"b":{"opcode":"control_forever","next":null,"parent":"a","inputs":{"SUBSTACK":[2,"c"]},"fields":{},"shadow":false,"topLevel":false},"c":{"opcode":"motion_movesteps","next":"d","parent":"b","inputs":{"STEPS":[1,[4,"10"]]},"fields":{},"shadow":false,"topLevel":false},"d":{"opcode":"motion_ifonedgebounce","next":null,"parent":"c","inputs":{},"fields":{},"shadow":false,"topLevel":false}},"comments":{},"currentCostume":0,"costumes":[{"name":"造型1","bitmapResolution":1,"dataFormat":"svg","assetId":"b7853f557e44241d288a7593e62c0d58","md5ext":"b7853f557e44241d288a7593e62c0d58.svg","rotationCenterX":48,"rotationCenterY":50}],"sounds":[],"volume":100,"visible":true,"x":0,"y":0,"size":100,"direction":90,"draggable":false,"rotationStyle":"all around","layerOrder":1}],"monitors":[],"extensions":[],"meta":{"semver":"3.0.0","vm":"0.2.0","agent":"Android"}}"""

import json

obj = json.loads(data)
b = obj["targets"][1]["blocks"]["b"]
print(b["inputs"]["SUBSTACK"])

