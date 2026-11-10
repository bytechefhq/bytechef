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

    it('shouldShow() only activates the suggestion when a function matches the query', () => {
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
        ] as never;

        const options = getFunctionSuggestionOptions();

        const context = {editor, range: {from: 1, to: 1}, transaction: editor.state.tr};

        expect(options.shouldShow!({...context, query: 'conc'} as never)).toBe(true);
        expect(options.shouldShow!({...context, query: 'true'} as never)).toBe(false);
    });

    it('command() inserts the call and parks the caret between the parentheses', () => {
        editor.commands.insertContent('con');

        const options = getFunctionSuggestionOptions();

        options.command!({editor, props: {name: 'concat'}, range: {from: 1, to: 4}} as never);

        expect(editor.state.doc.textContent).toBe('concat()');
        // from + 'concat'.length + 1 -> inside the parentheses.
        expect(editor.state.selection.from).toBe(8);
    });

    it('command() replaces the name of an existing call and keeps its arguments', () => {
        editor.commands.insertContent('getOrDefault(x, 0)');

        const options = getFunctionSuggestionOptions();

        options.command!({editor, props: {name: 'coalesce'}, range: {from: 1, to: 13}} as never);

        expect(editor.state.doc.textContent).toBe('coalesce(x, 0)');
        expect(editor.state.selection.from).toBe(10);
    });
});
