// 👤 Global DOM Interface Selectors
const lsMenu = document.getElementById('ls');
const overlayBg = document.getElementById('overlay');

// ⚙️ Gesture Engine Configuration Bounds
const MENU_WIDTH = 260; 
let isDragging = false;
let startTouchX = 0;

// 📂 Standardized Snap Animation States
function setMenuState(open) {
    if (!lsMenu || !overlayBg) return;

    lsMenu.style.transition = 'transform 0.3s cubic-bezier(0.1, 0.76, 0.55, 0.94)';

    if (open) {
        lsMenu.style.transform = 'translateX(0px)'; /* ✅ 0px brings it exactly flush to the right edge */
        lsMenu.classList.add('active');
        
        overlayBg.style.display = 'block';
        void overlayBg.offsetWidth; 
        overlayBg.classList.add('active');
    } else {
        lsMenu.style.transform = `translateX(${MENU_WIDTH}px)`; /* ✅ 260px slides it back out right side completely */
        lsMenu.classList.remove('active');
        
        overlayBg.classList.remove('active');
        setTimeout(() => {
            if (!lsMenu.classList.contains('active')) overlayBg.style.display = 'none';
        }, 300);
    }
}

// 📂 Toggle Menu Fallback Trigger
function toggleMenu() {
    if (!lsMenu) return;
    const isCurrentlyOpen = lsMenu.classList.contains('active');
    setMenuState(!isCurrentlyOpen);
}

// 🖐️ Live Real-Time Touch Interface Matrix Tracking
document.body.addEventListener('touchstart', (e) => {
    if (!lsMenu) return;

    const touchX = e.touches[0].clientX;
    const screenWidth = window.innerWidth;
    const isMenuOpen = lsMenu.classList.contains('active');

    // Precision 35px sensor zone on the outermost glass boundary frame
    if (isMenuOpen || touchX > (screenWidth - 35)) {
        isDragging = true;
        startTouchX = touchX;
        
        lsMenu.style.transition = 'none';
        if (overlayBg && !isMenuOpen) {
            overlayBg.style.display = 'block';
            void overlayBg.offsetWidth;
        }
    }
}, { passive: true });

document.body.addEventListener('touchmove', (e) => {
    if (!isDragging || !lsMenu) return;

    const touchX = e.touches[0].clientX;
    let deltaX = touchX - startTouchX;
    let targetX = MENU_WIDTH; // Default anchored safe coordinate state (hidden)

    const isMenuOpen = lsMenu.classList.contains('active');

    if (isMenuOpen) {
        // Sliding from Open to Closed (Moving finger right -> numbers grow positive from 0 to 260)
        if (deltaX < 0) deltaX = 0; 
        if (deltaX > MENU_WIDTH) deltaX = MENU_WIDTH;
        targetX = deltaX;
    } else {
        // Sliding from Closed to Open (Moving finger left -> numbers shrink down from 260 to 0)
        if (deltaX > 0) deltaX = 0; 
        if (deltaX < -MENU_WIDTH) deltaX = -MENU_WIDTH; 
        targetX = MENU_WIDTH + deltaX; // E.g., 260 + (-100px swipe) = 160px remaining offset position
    }

    lsMenu.style.transform = `translateX(${targetX}px)`;

    // Secure live opacity background scaling matching the new bounds inverted scale ratio
    if (overlayBg) {
        const opacityRatio = 1 - (targetX / MENU_WIDTH);
        overlayBg.style.opacity = opacityRatio;
    }
}, { passive: true });

document.body.addEventListener('touchend', (e) => {
    if (!isDragging || !lsMenu) return;
    isDragging = false;

    const endTouchX = e.changedTouches[0].clientX;
    const totalSwipeDelta = endTouchX - startTouchX;
    const isMenuOpen = lsMenu.classList.contains('active');

    if (isMenuOpen) {
        // If swiped right past threshold while open, throw it away
        if (totalSwipeDelta > 60 || totalSwipeDelta > (MENU_WIDTH / 2)) {
            setMenuState(false); 
        } else {
            setMenuState(true);  
        }
    } else {
        // If swiped left past threshold while closed, catch it open
        if (totalSwipeDelta < -60 || totalSwipeDelta < -(MENU_WIDTH / 2)) {
            setMenuState(true);  
        } else {
            setMenuState(false); 
        }
    }
}, { passive: true });
