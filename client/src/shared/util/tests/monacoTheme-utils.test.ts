import {afterEach, describe, expect, it, vi} from 'vitest';

import {MONACO_DARK_THEME, defineMonacoDarkTheme} from '../monacoTheme-utils';

import type {Monaco} from '@monaco-editor/react';

const createMonaco = () => {
    const defineTheme = vi.fn();

    return {defineTheme, monaco: {editor: {defineTheme}} as unknown as Monaco};
};

const mockComputedStyles = (tokenValues: Record<string, string>, probeColor: string) => {
    vi.spyOn(window, 'getComputedStyle').mockImplementation(
        (element: Element) =>
            (element === document.documentElement
                ? {getPropertyValue: (token: string) => tokenValues[token] ?? ''}
                : {color: probeColor}) as unknown as CSSStyleDeclaration
    );
};

describe('defineMonacoDarkTheme', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('defines the dark theme on top of vs-dark', () => {
        const {defineTheme, monaco} = createMonaco();

        defineMonacoDarkTheme(monaco, false);

        expect(defineTheme).toHaveBeenCalledWith(MONACO_DARK_THEME, {
            base: 'vs-dark',
            colors: {},
            inherit: true,
            rules: [],
        });
    });

    it('paints the editor colours with the resolved app tokens as hex', () => {
        const {defineTheme, monaco} = createMonaco();

        mockComputedStyles(
            {
                '--background': '240 10% 4%',
                '--border': '240 4% 16%',
                '--foreground': '0 0% 98%',
                '--muted': '240 4% 16%',
                '--muted-foreground': '240 5% 65%',
                '--popover': '240 10% 4%',
            },
            'rgb(9, 9, 11)'
        );

        defineMonacoDarkTheme(monaco);

        expect(defineTheme.mock.calls[0][1].colors).toEqual({
            'editor.background': '#09090b',
            'editor.foreground': '#09090b',
            'editor.lineHighlightBackground': '#09090b',
            'editorGutter.background': '#09090b',
            'editorIndentGuide.background': '#09090b',
            'editorLineNumber.foreground': '#09090b',
            'editorSuggestWidget.background': '#09090b',
            'editorWidget.background': '#09090b',
        });
    });

    it('skips colours whose token is not defined', () => {
        const {defineTheme, monaco} = createMonaco();

        mockComputedStyles({'--background': '240 10% 4%'}, 'rgba(9, 9, 11, 1)');

        defineMonacoDarkTheme(monaco);

        expect(defineTheme.mock.calls[0][1].colors).toEqual({
            'editor.background': '#09090b',
            'editorGutter.background': '#09090b',
        });
    });

    it('skips colours the browser does not resolve to rgb', () => {
        const {defineTheme, monaco} = createMonaco();

        mockComputedStyles({'--background': '240 10% 4%'}, '');

        defineMonacoDarkTheme(monaco);

        expect(defineTheme.mock.calls[0][1].colors).toEqual({});
    });

    it('removes its probe elements from the document', () => {
        const {monaco} = createMonaco();

        mockComputedStyles({'--background': '240 10% 4%'}, 'rgb(9, 9, 11)');

        defineMonacoDarkTheme(monaco);

        expect(document.body.querySelectorAll('span')).toHaveLength(0);
    });
});
