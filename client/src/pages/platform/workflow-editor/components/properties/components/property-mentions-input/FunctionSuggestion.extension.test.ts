import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/core';
import Document from '@tiptap/extension-document';
import Paragraph from '@tiptap/extension-paragraph';
import Text from '@tiptap/extension-text';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {FormulaMode} from './FormulaMode.extension';
import {FunctionSuggestion, FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';
import {getFunctionSuggestionOptions} from './functionSuggestionOptions';

vi.mock('./suggestionPopupRenderer', () => ({
    createSuggestionPopupRenderer: () => () => ({
        onExit: vi.fn(),
        onKeyDown: () => false,
        onStart: vi.fn(),
        onUpdate: vi.fn(),
    }),
}));

function functionDefinition(
    name: string,
    returnType = 'STRING',
    parameterTypes: string[] = []
): EvaluatorFunctionDefinition {
    return {
        category: 'STRING',
        description: '',
        example: '',
        name,
        parameters: parameterTypes.map((type, index) => ({
            description: '',
            name: `parameter${index}`,
            required: true,
            type,
        })),
        returnType,
        title: name,
    } as unknown as EvaluatorFunctionDefinition;
}

const FUNCTION_DEFINITIONS = [
    functionDefinition('concat'),
    functionDefinition('contains', 'BOOLEAN'),
    functionDefinition('uuid'),
    functionDefinition('size', 'INTEGER'),
    functionDefinition('getOrDefault', 'NUMBER', ['ANY', 'NUMBER']),
    functionDefinition('getOrDefault', 'STRING', ['ANY', 'STRING']),
];

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

    it('reopens the list on a word whose suggestion was dismissed', () => {
        editor.commands.setContent('<p>concat(uuid(), 1)</p>');
        editor.commands.setTextSelection(4);

        editor.view.dispatch(editor.state.tr.setMeta(FunctionSuggestionPluginKey, {exit: true}));

        editor.view.dom.dispatchEvent(new KeyboardEvent('keydown', {bubbles: true, ctrlKey: true, key: ' '}));

        expect(getSuggestionState(editor)?.active).toBe(true);
        expect(getSuggestionState(editor)?.query).toBe('con');
    });

    it('restarts an already active list so its popup opens again', () => {
        editor.commands.setContent('<p>concat(uuid(), 1)</p>');
        editor.commands.setTextSelection(4);

        const activeDecorationId = getSuggestionState(editor)?.decorationId;

        editor.view.dom.dispatchEvent(new KeyboardEvent('keydown', {bubbles: true, ctrlKey: true, key: ' '}));

        expect(getSuggestionState(editor)?.active).toBe(true);
        expect(getSuggestionState(editor)?.decorationId).not.toBe(activeDecorationId);
    });

    it('covers the whole word when opened in the middle of a function name', () => {
        editor.commands.setContent('<p>concat(uuid(), 1)</p>');
        editor.commands.setTextSelection(4);

        editor.commands.keyboardShortcut('Ctrl-Space');

        expect(getSuggestionState(editor)?.range).toEqual({from: 1, to: 7});
    });

    it('lists only functions that fit the argument at the caret', () => {
        editor.commands.setContent('<p>getOrDefault(uuid(), 1)</p>');
        editor.commands.setTextSelection(22);

        const items = getFunctionSuggestionOptions().items?.({
            editor,
            query: '',
            signal: new AbortController().signal,
        }) as EvaluatorFunctionDefinition[];

        expect(items.map((item) => item.name)).toEqual(['concat', 'uuid', 'size', 'getOrDefault', 'getOrDefault']);
    });

    it('does not open the list outside formula mode', () => {
        editor.destroy();

        editor = createEditor(false);

        editor.commands.keyboardShortcut('Ctrl-Space');

        expect(getSuggestionState(editor)?.active).toBe(false);
    });
});
