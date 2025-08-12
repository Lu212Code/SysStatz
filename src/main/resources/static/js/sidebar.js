function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  sidebar.classList.toggle('expanded');

  if (sidebar.classList.contains('expanded')) {
    document.body.classList.add('sidebar-expanded');
    localStorage.setItem('sidebarExpanded', 'true');
  } else {
    document.body.classList.remove('sidebar-expanded');
    localStorage.setItem('sidebarExpanded', 'false');
  }
}

window.addEventListener('DOMContentLoaded', () => {
  const sidebar = document.getElementById('sidebar');
  const body = document.body;

  // Transitionen vorübergehend deaktivieren
  body.classList.add('no-transition');
  sidebar.classList.add('no-transition');

  const expanded = localStorage.getItem('sidebarExpanded');

  if (expanded === 'true') {
    sidebar.classList.add('expanded');
    body.classList.add('sidebar-expanded');
  } else {
    sidebar.classList.remove('expanded');
    body.classList.remove('sidebar-expanded');
  }

  // Nach kurzer Verzögerung Transitionen wieder aktivieren
  setTimeout(() => {
    sidebar.classList.remove('no-transition');
    body.classList.remove('no-transition');
  }, 50);
});
