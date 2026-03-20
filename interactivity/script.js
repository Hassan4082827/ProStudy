function toggleMenu() {
            ls.classList.toggle('slide-in');
            overlay.classList.toggle('active');
        }
let startX = 0;

document.body.addEventListener('touchstart', (e) => {
    startX = e.touches[0].clientX;
});

document.body.addEventListener('touchend', (e) => {
    let endX = e.changedTouches[0].clientX;
    const menu = document.getElementById('ls');

    if ((endX - startX) > 70 && menu.classList.contains('slide-in')) {
        toggleMenu();
    }

    if ((startX - endX) > 70 && !menu.classList.contains('slide-in')) {
        toggleMenu();
    }
});