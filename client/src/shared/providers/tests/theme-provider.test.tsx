import {ThemeProvider, useTheme} from '@/shared/providers/theme-provider';
import {act, render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const changeListeners = new Set<(event: MediaQueryListEvent) => void>();

let systemPrefersDark = false;

const setSystemPrefersDark = (prefersDark: boolean) => {
    systemPrefersDark = prefersDark;

    changeListeners.forEach((listener) => listener({matches: prefersDark} as MediaQueryListEvent));
};

beforeEach(() => {
    changeListeners.clear();

    systemPrefersDark = false;

    localStorage.clear();

    document.documentElement.className = '';

    vi.stubGlobal('matchMedia', (query: string) => ({
        addEventListener: (_event: string, listener: (event: MediaQueryListEvent) => void) => {
            changeListeners.add(listener);
        },
        get matches() {
            return systemPrefersDark;
        },
        media: query,
        removeEventListener: (_event: string, listener: (event: MediaQueryListEvent) => void) => {
            changeListeners.delete(listener);
        },
    }));
});

describe('ThemeProvider', () => {
    it('applies the resolved system theme on mount', () => {
        render(
            <ThemeProvider defaultTheme="system">
                <div />
            </ThemeProvider>
        );

        expect(document.documentElement.classList.contains('light')).toBe(true);
    });

    it('follows the OS theme while the setting is system', () => {
        render(
            <ThemeProvider defaultTheme="system">
                <div />
            </ThemeProvider>
        );

        act(() => setSystemPrefersDark(true));

        expect(document.documentElement.classList.contains('dark')).toBe(true);
        expect(document.documentElement.classList.contains('light')).toBe(false);
    });

    it('ignores OS changes while the setting is an explicit theme', () => {
        render(
            <ThemeProvider defaultTheme="light">
                <div />
            </ThemeProvider>
        );

        act(() => setSystemPrefersDark(true));

        expect(document.documentElement.classList.contains('light')).toBe(true);
    });

    it('throws when useTheme is called outside a ThemeProvider', () => {
        const ThemeConsumer = () => {
            useTheme();

            return null;
        };

        expect(() => render(<ThemeConsumer />)).toThrow('useTheme must be used within a ThemeProvider');
    });
});
