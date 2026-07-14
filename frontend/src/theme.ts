// Author: huangbingrui.awa

export type ColorTheme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'ojtdm.theme';

function storedTheme(): ColorTheme | null {
  try {
    const value = window.localStorage.getItem(THEME_STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : null;
  } catch {
    return null;
  }
}

function systemTheme(): ColorTheme {
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function applyTheme(theme: ColorTheme): ColorTheme {
  const root = document.documentElement;
  root.dataset.theme = theme;
  root.classList.toggle('dark', theme === 'dark');
  root.style.colorScheme = theme;
  return theme;
}

export function initializeTheme(): ColorTheme {
  return applyTheme(storedTheme() ?? systemTheme());
}

export function currentTheme(): ColorTheme {
  const applied = document.documentElement.dataset.theme;
  return applied === 'dark' || applied === 'light' ? applied : initializeTheme();
}

export function setTheme(theme: ColorTheme): void {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Applying the theme still works when storage is unavailable.
  }
  applyTheme(theme);
}

export function toggleTheme(): ColorTheme {
  const next = currentTheme() === 'dark' ? 'light' : 'dark';
  setTheme(next);
  return next;
}

export function installThemeSync(): () => void {
  const handleStorage = (event: StorageEvent) => {
    if (event.key !== null && event.key !== THEME_STORAGE_KEY) return;
    applyTheme(event.newValue === 'dark' || event.newValue === 'light' ? event.newValue : systemTheme());
  };
  window.addEventListener('storage', handleStorage);
  return () => window.removeEventListener('storage', handleStorage);
}
