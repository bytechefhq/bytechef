import {applyHubTheme} from '@/ee/pages/embedded/automation-hub/theme/applyHubTheme';
import {beforeEach, describe, expect, it} from 'vitest';

describe('applyHubTheme', () => {
    let root: HTMLElement;

    beforeEach(() => {
        root = document.createElement('div');
    });

    it('sets primary, ring, primary-foreground, radius and font CSS variables and returns light', () => {
        const mode = applyHubTheme({borderRadius: '4px', fontFamily: 'Inter', primaryColor: '#ff0000'}, root);

        expect(root.style.getPropertyValue('--primary')).toBe('#ff0000');
        expect(root.style.getPropertyValue('--ring')).toBe('#ff0000');
        expect(root.style.getPropertyValue('--primary-foreground')).toBe('#ffffff');
        expect(root.style.getPropertyValue('--radius')).toBe('4px');
        expect(root.style.getPropertyValue('--font-sans')).toBe('Inter');
        expect(mode).toBe('light');
    });

    it('returns dark when mode is dark', () => {
        const mode = applyHubTheme({mode: 'dark'}, root);

        expect(mode).toBe('dark');
    });

    it('leaves --primary unset when primaryColor is not a supported color', () => {
        applyHubTheme({primaryColor: 'not-a-color'}, root);

        expect(root.style.getPropertyValue('--primary')).toBe('');
    });

    it('accepts a 3-digit hex primaryColor and computes contrast from its expanded form', () => {
        applyHubTheme({primaryColor: '#f00'}, root);

        expect(root.style.getPropertyValue('--primary')).toBe('#f00');
        expect(root.style.getPropertyValue('--ring')).toBe('#f00');
        expect(root.style.getPropertyValue('--primary-foreground')).toBe('#ffffff');
    });

    it('leaves --radius unset when borderRadius is not a supported CSS length', () => {
        applyHubTheme({borderRadius: 'not-a-length'}, root);

        expect(root.style.getPropertyValue('--radius')).toBe('');
    });
});
