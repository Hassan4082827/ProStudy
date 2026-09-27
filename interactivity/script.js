// 👤 Global DOM Interface Selectors
const lsMenu = document.getElementById('ls');
const overlayBg = document.getElementById('overlay');
const THEME_STORAGE_KEY = 'prostudy_theme';

function resolveEffectiveTheme(theme) {
    if (theme === 'dark') return 'dark';
    if (theme === 'light') return 'light';

    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function applyTheme(theme) {
    const normalizedTheme = (theme === 'dark' || theme === 'light' || theme === 'system') ? theme : 'system';
    const effectiveTheme = resolveEffectiveTheme(normalizedTheme);
    const isDark = effectiveTheme === 'dark';

    document.body.classList.toggle('dark-mode', isDark);
    document.documentElement.setAttribute('data-theme', effectiveTheme);
    localStorage.setItem(THEME_STORAGE_KEY, normalizedTheme);

    const themeOptions = document.querySelectorAll('input[name="theme-mode"]');
    themeOptions.forEach((option) => {
        const matches = option.value === normalizedTheme;
        option.checked = matches;
    });

    const status = document.getElementById('theme-status');
    if (status) {
        const label = normalizedTheme === 'system'
            ? (isDark ? 'System (Default): dark' : 'System (Default): light')
            : normalizedTheme === 'dark'
                ? 'Dark mode enabled'
                : 'Light mode enabled';
        status.textContent = label;
    }
}

function initializeTheme() {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    const theme = savedTheme || 'system';
    applyTheme(theme);

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const updateFromSystem = () => {
        const currentTheme = localStorage.getItem(THEME_STORAGE_KEY) || 'system';
        if (currentTheme === 'system') {
            applyTheme('system');
        }
    };

    if (typeof mediaQuery.addEventListener === 'function') {
        mediaQuery.addEventListener('change', updateFromSystem);
    } else if (typeof mediaQuery.addListener === 'function') {
        mediaQuery.addListener(updateFromSystem);
    }
}

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

initializeTheme();

function setSubjectPopupState(activeTrigger) {
    document.querySelectorAll('.subject-trigger').forEach((item) => {
        item.classList.toggle('open', item === activeTrigger);
    });
}

function normalizeMetadataKey(value) {
    return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

function getMetadataUrl() {
    return 'https://cdn.jsdelivr.net/gh/hassan4082827/ProStudyStorage@main/metadata.json';
}

function ensureToastContainer() {
    let container = document.getElementById('homework-toast-container');
    if (container) return container;

    container = document.createElement('div');
    container.id = 'homework-toast-container';
    container.className = 'homework-toast-container';
    document.body.appendChild(container);
    return container;
}

function showToast(message) {
    const container = ensureToastContainer();
    const toast = document.createElement('div');
    toast.className = 'homework-toast';
    toast.textContent = message;
    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.add('show');
    });

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 220);
    }, 2200);
}

function ensureSubjectActionButton(row) {
    if (row.querySelector('.subject-trigger')) {
        return row.querySelector('.subject-trigger');
    }

    const originalIcon = row.querySelector('.icon-sq');
    if (!originalIcon) return null;

    const iconClone = originalIcon.cloneNode(true);
    originalIcon.remove();

    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.className = 'subject-icon-button';
    trigger.setAttribute('aria-label', 'Check homework availability');
    trigger.appendChild(iconClone);

    const wrapper = document.createElement('div');
    wrapper.className = 'subject-trigger';
    wrapper.appendChild(trigger);
    row.prepend(wrapper);

    return wrapper;
}

function attachHomeworkDownloadAction(row, fileUrl, fileName) {
    if (!fileUrl) return;

    const wrapper = ensureSubjectActionButton(row);
    if (!wrapper) return;

    const trigger = wrapper.querySelector('.subject-icon-button');
    const existingOverlay = wrapper.querySelector('.subject-overlay');
    if (existingOverlay) {
        existingOverlay.remove();
    }

    const overlay = document.createElement('div');
    overlay.className = 'subject-overlay';
    overlay.innerHTML = `
        <a href="${fileUrl}" download="${fileName}" class="subject-action download-btn" aria-label="Download attachment">
            <i class="fa fa-download"></i> Download
        </a>
        <button type="button" class="subject-action cancel-btn" aria-label="Close subject action">
            <i class="fa fa-xmark"></i>
        </button>
    `;

    wrapper.appendChild(overlay);

    trigger.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        const isOpen = wrapper.classList.contains('open');
        setSubjectPopupState(isOpen ? null : wrapper);
    });

    overlay.querySelector('.cancel-btn').addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        setSubjectPopupState(null);
    });

    overlay.querySelector('.download-btn').addEventListener('click', (event) => {
        event.stopPropagation();
        setSubjectPopupState(null);
    });
}

function attachHomeworkUnavailableAction(row) {
    const wrapper = ensureSubjectActionButton(row);
    if (!wrapper) return;

    const trigger = wrapper.querySelector('.subject-icon-button');
    const subjectText = row.querySelector('.info h3')?.textContent?.trim() || 'Subject';

    trigger.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        setSubjectPopupState(null);
        showToast(`${subjectText}: Not Available`);
    });
}

async function initializeHomeworkSubjectActions() {
    const itemRows = document.querySelectorAll('.item-row');

    try {
        const response = await fetch(getMetadataUrl(), { cache: 'no-store' });
        if (!response.ok) {
            throw new Error(`metadata request failed: ${response.status}`);
        }

        const data = await response.json();
        const records = Array.isArray(data.homework)
            ? data.homework
            : Array.isArray(data.assignments)
                ? data.assignments
                : [];

        const recordMap = new Map();
        records.forEach((record) => {
            const dateKey = normalizeMetadataKey(record.date);
            const subjectKey = normalizeMetadataKey(record.subject);
            const lookupKey = `${dateKey}|${subjectKey}`;

            if (record.fileUrl) {
                recordMap.set(lookupKey, record);
            }
        });

        itemRows.forEach((row) => {
            const dateText = row.closest('.date-group')?.querySelector('.date-header')?.textContent || '';
            const subjectText = row.querySelector('.info h3')?.textContent || '';
            const lookupKey = `${normalizeMetadataKey(dateText)}|${normalizeMetadataKey(subjectText)}`;
            const record = recordMap.get(lookupKey);

            if (record && record.fileUrl) {
                attachHomeworkDownloadAction(row, record.fileUrl, record.fileName || record.name || 'study_material.pdf');
                return;
            }

            attachHomeworkUnavailableAction(row);
        });
    } catch (error) {
        console.warn('Homework metadata not loaded:', error);
    }

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.subject-trigger')) {
            setSubjectPopupState(null);
        }
    });
}

initializeHomeworkSubjectActions();

const themeOptions = document.querySelectorAll('input[name="theme-mode"]');
themeOptions.forEach((option) => {
    option.addEventListener('change', (event) => {
        if (event.target.checked) {
            applyTheme(event.target.value);
        }
    });
});

