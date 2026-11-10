import Document from '@tiptap/extension-document';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Text} from '@tiptap/extension-text';
import {Editor} from '@tiptap/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {FormulaMode} from './FormulaMode.extension';
import {FunctionSignature} from './FunctionSignature.extension';
import {FunctionSuggestion, FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';

vi.mock('./suggestionPopupRenderer', () => ({
    createSuggestionPopupRenderer: () => () => ({
        onExit: vi.fn(),
        onKeyDown: () => false,
        onStart: vi.fn(),
        onUpdate: vi.fn(),
    }),
}));

const definitions = [
    {
        category: 'STRING',
        description: '',
        example: '',
        name: 'contains',
        parameters: [
            {description: '', name: 'string', required: true, type: 'STRING'},
            {description: '', name: 'substring', required: true, type: 'STRING'},
        ],
        returnType: 'BOOLEAN',
        title: 'contains',
    },
    {
        category: 'STRING',
        description: '',
        example: '',
        name: 'uuid',
        parameters: [],
        returnType: 'STRING',
        title: 'uuid',
    },
] as never;

const tippyRoots = () => document.querySelectorAll('[data-tippy-root]');

function pressKey(editor: Editor, init: KeyboardEventInit) {
    editor.view.dom.dispatchEvent(new KeyboardEvent('keydown', {bubbles: true, ...init}));
}

describe('FunctionSignature', () => {
    let editor: Editor;

    beforeEach(() => {
        const element = document.createElement('div');

        document.body.appendChild(element);

        editor = new Editor({
            element,
            extensions: [
                Document,
                Paragraph,
                Text,
                FormulaMode.configure({getIsFormulaMode: () => true, initialFormulaMode: true}),
                FunctionSuggestion,
                FunctionSignature,
            ],
        });

        vi.spyOn(editor, 'isFocused', 'get').mockReturnValue(true);

        editor.storage.FunctionSuggestion.functionDefinitions = definitions;

        editor.commands.insertContent('contains(a, )');
        editor.commands.setTextSelection(editor.state.selection.from - 1);
    });

    afterEach(() => {
        editor.destroy();

        document.body.innerHTML = '';
    });

    it('shows the signature tooltip while the caret is inside a call', () => {
        expect(tippyRoots().length).toBe(1);
    });

    it('hides the signature tooltip while the function list is open', () => {
        pressKey(editor, {ctrlKey: true, key: ' '});

        expect(FunctionSuggestionPluginKey.getState(editor.state)?.active).toBe(true);
        expect(tippyRoots().length).toBe(0);
    });

    it('shows the signature tooltip again once the function list closes', () => {
        pressKey(editor, {ctrlKey: true, key: ' '});
        pressKey(editor, {key: 'Escape'});

        expect(FunctionSuggestionPluginKey.getState(editor.state)?.active).toBe(false);
        expect(tippyRoots().length).toBe(1);
    });
});
