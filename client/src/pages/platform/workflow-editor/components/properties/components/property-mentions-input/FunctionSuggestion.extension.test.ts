import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/core';
import Document from '@tiptap/extension-document';
import Paragraph from '@tiptap/extension-paragraph';
import Text from '@tiptap/extension-text';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {FormulaMode} from './FormulaMode.extension';
import {FunctionSuggestion, FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';

vi.mock('./suggestionPopupRenderer', () => ({
    createSuggestionPopupRenderer: () => () => ({
        onExit: vi.fn(),
        onKeyDown: () => false,
        onStart: vi.fn(),
        onUpdate: vi.fn(),
    }),
}));

const FUNCTION_DEFINITIONS = ['concat', 'contains', 'uuid'].map(
    (name) =>
        ({
            category: 'STRING',
            description: '',
            example: '',
            name,
            parameters: [],
            returnType: 'STRING',
            title: name,
        }) as unknown as EvaluatorFunctionDefinition
);

function createEditor(isFormulaMode: boolean): Editor {
    const element = document.createElement('div');

    document.body.appendChild(element);

    const editor = new Editor({
        element,
        extensions: [
            Document,
            Paragraph,
            Text,
            FormulaMode.configure({getIsFormulaMode: () => isFormulaMode, setIsFormulaMode: () => {}}),
            FunctionSuggestion,
        ],
    });

    Object.defineProperty(editor, 'isFocused', {get: () => true});

    editor.storage.FunctionSuggestion.functionDefinitions = FUNCTION_DEFINITIONS;

    return editor;
}

function getSuggestionState(editor: Editor) {
    return FunctionSuggestionPluginKey.getState(editor.state);
}

describe('FunctionSuggestion Ctrl+Space', () => {
    let editor: Editor;

    beforeEach(() => {
        editor = createEditor(true);
    });

    afterEach(() => {
        editor.destroy();

        document.body.innerHTML = '';
    });

    it('opens the full function list in an empty formula', () => {
        expect(getSuggestionState(editor)?.active).toBe(false);

        editor.commands.keyboardShortcut('Ctrl-Space');

        expect(getSuggestionState(editor)?.active).toBe(true);
        expect(getSuggestionState(editor)?.query).toBe('');
    });

    it('narrows the opened list as a single character is typed', () => {
        editor.commands.keyboardShortcut('Ctrl-Space');

        editor.commands.insertContent('c');

        expect(getSuggestionState(editor)?.active).toBe(true);
        expect(getSuggestionState(editor)?.query).toBe('c');
    });

    it('closes the opened list once the text stops being a word', () => {
        editor.commands.keyboardShortcut('Ctrl-Space');

        editor.commands.insertContent('(');

        expect(getSuggestionState(editor)?.active).toBe(false);
        expect(editor.storage.FunctionSuggestion.forcedFrom).toBeNull();
    });

    it('does not open the list outside formula mode', () => {
        editor.destroy();

        editor = createEditor(false);

        editor.commands.keyboardShortcut('Ctrl-Space');

        expect(getSuggestionState(editor)?.active).toBe(false);
    });
});
