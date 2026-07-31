with open("app/src/main/assets/scratch_blocks_viewer.html", "r") as f:
    content = f.read()

target_ui = """    const cls = getCategoryClassByOpcode(b.opcode);
    html += `<div class="block ${cls}" style="justify-content:space-between;width:100%;">
      <span>${b.label}</span>
      <span style="font-size:12px;opacity:0.8;margin-left:8px;" onclick="event.stopPropagation();removeBlock(${idx})">✕</span>
    </div>`;"""

replacement_ui = """    const cls = getCategoryClassByOpcode(b.opcode);
    if (window.isViewOnly) {
      html += `<div class="block ${cls}" style="justify-content:space-between;width:100%;">
        <span>${b.label}</span>
      </div>`;
    } else {
      html += `<div class="block ${cls}" style="justify-content:space-between;width:100%;">
        <span>${b.label}</span>
        <span class="remove-btn" style="font-size:12px;opacity:0.8;margin-left:8px;cursor:pointer;" onclick="event.stopPropagation();removeBlock(${idx})">✕</span>
      </div>`;
    }"""

content = content.replace(target_ui, replacement_ui)

target_script_end = """window.onload = function() {"""

replacement_script_end = """window.setViewOnly = function(isViewOnly) {
  window.isViewOnly = isViewOnly;
  if(isViewOnly) {
    var sidebar = document.querySelector('.category-sidebar');
    if(sidebar) sidebar.style.display = 'none';
    var palette = document.querySelector('.palette-panel');
    if(palette) palette.style.display = 'none';
    updateWorkspaceUI();
  }
};

window.onload = function() {"""

content = content.replace(target_script_end, replacement_script_end)

with open("app/src/main/assets/scratch_blocks_viewer.html", "w") as f:
    f.write(content)
