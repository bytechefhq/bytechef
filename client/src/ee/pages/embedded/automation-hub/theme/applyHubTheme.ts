import {AutomationHubThemeI} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';

const HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

function contrastForeground(hex: string): string {
    const full = hex.length === 4 ? `#${[...hex.slice(1)].map((char) => char + char).join('')}` : hex;
    const [red, green, blue] = [1, 3, 5].map((offset) => parseInt(full.slice(offset, offset + 2), 16) / 255);
    const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

    return luminance > 0.5 ? '#111111' : '#ffffff';
}

const CSS_LENGTH = /^\d+(\.\d+)?(px|rem|em|%)$/;

function isSupportedColor(value: string): boolean {
    if (HEX_COLOR.test(value)) {
        return true;
    }

    // jsdom has no CSS.supports; non-hex colors are only accepted where the browser can vouch for them
    return typeof CSS !== 'undefined' && typeof CSS.supports === 'function' && CSS.supports('color', value);
}

export function applyHubTheme(
    theme: AutomationHubThemeI,
    root: HTMLElement = document.documentElement
): 'dark' | 'light' {
    if (theme.primaryColor && isSupportedColor(theme.primaryColor)) {
        root.style.setProperty('--primary', theme.primaryColor);
        root.style.setProperty('--ring', theme.primaryColor);

        if (HEX_COLOR.test(theme.primaryColor)) {
            root.style.setProperty('--primary-foreground', contrastForeground(theme.primaryColor));
        }
    }

    if (theme.fontFamily) {
        root.style.setProperty('--font-sans', theme.fontFamily);
    }

    if (theme.borderRadius && CSS_LENGTH.test(theme.borderRadius)) {
        root.style.setProperty('--radius', theme.borderRadius);
    }

    return theme.mode === 'dark' ? 'dark' : 'light';
}
