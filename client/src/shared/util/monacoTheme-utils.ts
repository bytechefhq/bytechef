import type {Monaco} from '@monaco-editor/react';

export const MONACO_DARK_THEME = 'bytechef-dark';
export const MONACO_LIGHT_THEME = 'light';

const resolveCssColor = (cssColor: string): string | undefined => {
    const probeElement = document.createElement('span');

    probeElement.style.color = cssColor;
    probeElement.style.display = 'none';

    document.body.appendChild(probeElement);

    const computedColor = window.getComputedStyle(probeElement).color;

    probeElement.remove();

    const rgbMatch = /^rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(computedColor);

    if (!rgbMatch) {
        return undefined;
    }

    return `#${[1, 2, 3].map((group) => Number(rgbMatch[group]).toString(16).padStart(2, '0')).join('')}`;
};

const resolveToken = (token: string): string | undefined => {
    const rawValue = window.getComputedStyle(document.documentElement).getPropertyValue(token);

    if (!rawValue.trim()) {
        return undefined;
    }

    return resolveCssColor(`hsl(var(${token}))`);
};

export const defineMonacoDarkTheme = (monaco: Monaco, withColors = true): void => {
    const colors: Record<string, string> = {};

    if (withColors) {
        const tokenColors: Record<string, string | undefined> = {
            'editor.background': resolveToken('--background'),
            'editor.foreground': resolveToken('--foreground'),
            'editor.lineHighlightBackground': resolveToken('--muted'),
            'editorGutter.background': resolveToken('--background'),
            'editorIndentGuide.background': resolveToken('--border'),
            'editorLineNumber.foreground': resolveToken('--muted-foreground'),
            'editorSuggestWidget.background': resolveToken('--popover'),
            'editorWidget.background': resolveToken('--popover'),
        };

        for (const [colorId, color] of Object.entries(tokenColors)) {
            if (color) {
                colors[colorId] = color;
            }
        }
    }

    monaco.editor.defineTheme(MONACO_DARK_THEME, {
        base: 'vs-dark',
        colors,
        inherit: true,
        rules: [],
    });
};
