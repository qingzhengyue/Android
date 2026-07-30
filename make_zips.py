import zipfile
import os

os.makedirs('app/src/main/assets/mock_works', exist_ok=True)

project_json = """{
  "targets": [
    {
      "isStage": true,
      "name": "Stage",
      "blocks": {
         "event_whenflagclicked": {
            "opcode": "event_whenflagclicked",
            "next": "looks_say",
            "parent": null,
            "inputs": {},
            "fields": {},
            "shadow": false,
            "topLevel": true
         },
         "looks_say": {
            "opcode": "looks_say",
            "next": null,
            "parent": "event_whenflagclicked",
            "inputs": {
                "MESSAGE": [1, [10, "Hello!"]]
            },
            "fields": {},
            "shadow": false,
            "topLevel": false
         }
      }
    }
  ],
  "meta": { "semver": "3.0.0" }
}"""

for name in ['space_walk.sb3', 'cat_stroll.sb3', 'maze.sb3', 'default_work.sb3']:
    with zipfile.ZipFile(os.path.join('app/src/main/assets/mock_works', name), 'w') as zf:
        zf.writestr('project.json', project_json.replace('Hello!', f'This is {name}'))

print("Created zip files successfully.")
