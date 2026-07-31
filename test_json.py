import json

data = {"inputs":{"SUBSTACK":["b3"]}}
inputs = data.get("inputs")
substack = inputs.get("SUBSTACK")
print(substack)
print(len(substack))
