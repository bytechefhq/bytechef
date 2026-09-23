import {AutomationHubThemeI} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';

const HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

const CSS_LENGTH = /^\d+(\.\d+)?(px|rem|em|%)$/;

function isSupportedColor(value: string): boolean {
    if (HEX_COLOR.test(value)) {
        return true;
    }

    return typeof CSS !== 'undefined' && typeof CSS.supports === 'function' && CSS.supports('color', value);
}

const SURFACE_VARIABLES: Record<string, string> = {
    activeBorderColor: '--hub-active-border',
    cardColor: '--hub-card',
    disableColor: '--hub-disable',
    enableColor: '--hub-enable',
    onAccentColor: '--hub-on-accent',
    segmentColor: '--hub-segment-selected',
    surfaceColor: '--hub-surface',
};

const HOVER_VARIABLES: Record<string, string> = {
    disableColor: '--hub-disable-hover',
    enableColor: '--hub-enable-hover',
};

function toRgb(color: string): [number, number, number] | undefined {
    if (HEX_COLOR.test(color)) {
        const full = color.length === 4 ? `#${Array.from(color.slice(1), (char) => char + char).join('')}` : color;

        return [1, 3, 5].map((offset) => Number.parseInt(full.slice(offset, offset + 2), 16)) as [
            number,
            number,
            number,
        ];
    }

    if (typeof document === 'undefined') {
        return undefined;
    }

    const probe = document.createElement('span');

    probe.style.color = color;

    const match = /^rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(probe.style.color);

    return match ? [Number(match[1]), Number(match[2]), Number(match[3])] : undefined;
}

function toHslTriplet(color: string): string | undefined {
    const rgb = toRgb(color);

    if (!rgb) {
        return undefined;
    }

    const [red, green, blue] = rgb.map((channel) => channel / 255);
    const max = Math.max(red, green, blue);
    const min = Math.min(red, green, blue);
    const lightness = (max + min) / 2;

    let hue = 0;
    let saturation = 0;

    if (max !== min) {
        const delta = max - min;

        saturation = lightness > 0.5 ? delta / (2 - max - min) : delta / (max + min);

        if (max === red) {
            hue = (green - blue) / delta + (green < blue ? 6 : 0);
        } else if (max === green) {
            hue = (blue - red) / delta + 2;
        } else {
            hue = (red - green) / delta + 4;
        }

        hue *= 60;
    }

    return `${Math.round(hue)} ${Math.round(saturation * 100)}% ${Math.round(lightness * 100)}%`;
}

function contrastForeground(color: string): string | undefined {
    const rgb = toRgb(color);

    if (!rgb) {
        return undefined;
    }

    const [red, green, blue] = rgb.map((channel) => channel / 255);
    const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

    return toHslTriplet(luminance > 0.5 ? '#111111' : '#ffffff');
}

const BUILDER_VARIABLES: Record<string, string[]> = {
    cardColor: ['--background', '--card', '--popover', '--surface-neutral-primary'],
    surfaceColor: ['--muted', '--surface-main'],
};

const BRAND_VARIABLES = [
    '--primary',
    '--ring',
    '--surface-brand-primary',
    '--surface-brand-primary-hover',
    '--surface-brand-primary-active',
];

function applyBrandColor(primaryColor: string, root: HTMLElement) {
    const brandTriplet = toHslTriplet(primaryColor);
    const foregroundTriplet = contrastForeground(primaryColor);

    if (brandTriplet) {
        for (const variable of BRAND_VARIABLES) {
            root.style.setProperty(variable, brandTriplet);
        }
    }

    if (foregroundTriplet) {
        root.style.setProperty('--primary-foreground', foregroundTriplet);
    }
}

function applySurfaceColor(themeKey: string, variable: string, value: string, root: HTMLElement) {
    root.style.setProperty(variable, value);

    const triplet = toHslTriplet(value);

    if (triplet) {
        for (const builderVariable of BUILDER_VARIABLES[themeKey] ?? []) {
            root.style.setProperty(builderVariable, triplet);
        }
    }

    const hoverVariable = HOVER_VARIABLES[themeKey];

    if (hoverVariable) {
        root.style.setProperty(hoverVariable, value);
    }
}

export function applyHubTheme(
    theme: AutomationHubThemeI,
    root: HTMLElement = document.documentElement
): 'dark' | 'light' {
    if (theme.primaryColor && isSupportedColor(theme.primaryColor)) {
        applyBrandColor(theme.primaryColor, root);
    }

    if (theme.fontFamily) {
        root.style.setProperty('--font-sans', theme.fontFamily);
    }

    if (theme.borderRadius && CSS_LENGTH.test(theme.borderRadius)) {
        root.style.setProperty('--radius', theme.borderRadius);
    }

    for (const [themeKey, variable] of Object.entries(SURFACE_VARIABLES)) {
        const value = (theme as Record<string, unknown>)[themeKey];

        if (typeof value === 'string' && isSupportedColor(value)) {
            applySurfaceColor(themeKey, variable, value, root);
        }
    }

    for (const [variable, value] of Object.entries(theme.cssVariables ?? {})) {
        if (variable.startsWith('--') && typeof value === 'string') {
            root.style.setProperty(variable, value);
        }
    }

    return theme.mode === 'dark' ? 'dark' : 'light';
}
