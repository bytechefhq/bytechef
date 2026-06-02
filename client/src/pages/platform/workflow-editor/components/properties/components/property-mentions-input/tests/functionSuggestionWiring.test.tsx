import Document from '@tiptap/extension-document';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Text} from '@tiptap/extension-text';
import {Editor} from '@tiptap/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {FunctionSuggestion} from '../FunctionSuggestion.extension';
import {getFunctionSuggestionOptions} from '../functionSuggestionOptions';

vi.mock('tippy.js', () => ({default: vi.fn(() => [{popper: {style: {}}}])}));

describe('FunctionSuggestion wiring', () => {
    let editor: Editor;

    beforeEach(() => {
        editor = new Editor({extensions: [Document, FunctionSuggestion, Paragraph, Text]});
    });

    it('exposes a FunctionSuggestion storage bucket initialised to an empty catalog', () => {
        expect(editor.storage.FunctionSuggestion.functionDefinitions).toEqual([]);
    });

    it('accepts a catalog pushed into storage', () => {
        editor.storage.FunctionSuggestion.functionDefinitions = [{name: 'concat'}] as never;

        expect(editor.storage.FunctionSuggestion.functionDefinitions).toHaveLength(1);
    });

    it('items() reads the catalog from storage and filters by query', () => {
        editor.storage.FunctionSuggestion.functionDefinitions = [
            {
                category: 'STRING',
                description: '',
                example: '',
                name: 'concat',
                parameters: [],
                returnType: 'STRING',
                title: 'concat',
            },
            {
                category: 'STRING',
                description: '',
                example: '',
                name: 'substring',
                parameters: [],
                returnType: 'STRING',
                title: 'substring',
            },
        ] as never;

        const options = getFunctionSuggestionOptions();
        const items = options.items!({editor, query: 'conc'} as never) as Array<{name: string}>;

        expect(items.map((item) => item.name)).toEqual(['concat']);
    });

    it('allow() is false when formula mode is not active', () => {
        const options = getFunctionSuggestionOptions();

        // FormulaMode extension is not registered here, so the flag is absent -> dormant.
        expect(options.allow!({editor, isActive: false, range: {from: 0, to: 0}} as never)).toBe(false);
    });
});
