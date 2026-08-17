import Document from '@tiptap/extension-document';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Text} from '@tiptap/extension-text';
import {Editor} from '@tiptap/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {FormulaMode} from '../FormulaMode.extension';
import {FunctionSignature} from '../FunctionSignature.extension';
import {FunctionSuggestion} from '../FunctionSuggestion.extension';

// The real ReactRenderer renders through `flushSync`, which drains pending React effects. One of them
// is tiptap's own useEditor effect calling `editor.setOptions`, which runs `view.setProps` ->
// `updatePluginViews` -> the signature plugin's `update` — re-entering it while the outer call is
// still constructing its renderer. This double reproduces that single re-entrant call.
vi.mock('@tiptap/react', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tiptap/react')>();

    let reentered = false;

    class ReentrantReactRenderer extends actual.ReactRenderer {
        constructor(...args: ConstructorParameters<typeof actual.ReactRenderer>) {
            super(...args);

            if (!reentered) {
                reentered = true;

                args[1].editor.setOptions({});
            }
        }
    }

    return {...actual, ReactRenderer: ReentrantReactRenderer};
});

const definitions = [
    {
        category: 'STRING',
        description: 'Checks whether a string contains a substring',
        example: '',
        name: 'contains',
        parameters: [
            {description: 'The string to search in', name: 'string', required: true, type: 'STRING'},
            {description: 'The substring', name: 'substring', required: true, type: 'STRING'},
        ],
        returnType: 'BOOLEAN',
        title: 'contains',
    },
] as never;

let editor: Editor | undefined;

const tippyRoots = () => document.querySelectorAll('[data-tippy-root]');

afterEach(() => {
    editor?.destroy();

    editor = undefined;

    document.body.innerHTML = '';
});

describe('FunctionSignature tooltip lifecycle', () => {
    it('does not orphan a popper when update re-enters while the tooltip is being created', () => {
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

        // Mirrors what the suggestion command inserts: "name()" with the caret between the parens.
        editor.commands.insertContent('contains()');
        editor.commands.setTextSelection(editor.state.selection.from - 1);

        expect(tippyRoots().length, 'poppers while the caret sits inside contains()').toBe(1);

        // Every popper must still be reachable through its instance, or nothing can ever hide it.
        editor.commands.setTextSelection(editor.state.doc.content.size - 1);

        expect(tippyRoots().length, 'poppers after the caret leaves the call').toBe(0);
    });
});
