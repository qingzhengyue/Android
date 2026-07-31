with open("app/src/main/assets/scratch_blocks_viewer.html", "r") as f:
    content = f.read()

target = """window.setViewOnly = function(isViewOnly) {
  window.isViewOnly = isViewOnly;
  if(isViewOnly) {
    var sidebar = document.querySelector('.category-sidebar');
    if(sidebar) sidebar.style.display = 'none';
    var palette = document.querySelector('.palette-panel');
    if(palette) palette.style.display = 'none';
    updateWorkspaceUI();
  }
};"""

replacement = """window.setViewOnly = function(isViewOnly) {
  window.isViewOnly = isViewOnly;
  if(isViewOnly) {
    var sidebar = document.querySelector('.category-sidebar');
    if(sidebar) sidebar.style.display = 'none';
    var palette = document.querySelector('.palette-panel');
    if(palette) palette.style.display = 'none';
    var header = document.querySelector('.workspace-header');
    if(header) header.style.display = 'none';
    updateWorkspaceUI();
  }
};"""

content = content.replace(target, replacement)

with open("app/src/main/assets/scratch_blocks_viewer.html", "w") as f:
    f.write(content)
