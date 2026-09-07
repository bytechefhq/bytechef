import {fireEvent} from '@testing-library/react';
import Document from '@tiptap/extension-document';
import Paragraph from '@tiptap/extension-paragraph';
import Text from '@tiptap/extension-text';
import {Editor} from '@tiptap/react';
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {FormulaMode, FormulaModeOptionsI} from '../FormulaMode.extension';

/**
 * The FormulaMode extension's contract, exercised against a real editor.
 *
 * Emptying the field is what leaves formula mode. The handler reads the current mode through a getter rather
 * than the extension's own storage, because storage resets to its default whenever the extensions are rebuilt
 * (a component icon arriving from a fetch is enough) and the sync effect has not necessarily run by the time
 * the key lands.
 */

let element: HTMLElement;
let editor: Editor;

// The editor creates its view on a timer, so onCreate - and with it the storage seeding - has not run when
// the constructor returns. That window is precisely why the handler reads the mode through a getter.
const createEditor = async ({content = '', options}: {content?: string; options: Partial<FormulaModeOptionsI>}) => {
    element = document.createElement('div');

    document.body.appendChild(element);

    editor = new Editor({
        content,
        element,
        extensions: [Document, Paragraph, Text, FormulaMode.configure(options as FormulaModeOptionsI)],
    });

    await new Promise((resolve) => setTimeout(resolve, 0));

    return editor;
};

const pressKey = (key: string, keyCode: number) =>
    fireEvent.keyDown(editor.view.dom, {charCode: keyCode, code: key, key, keyCode});

describe('FormulaMode extension', () => {
    let setIsFormulaMode: Mock<(value: boolean) => void>;
    let saveNullValue: Mock<() => void>;

    beforeEach(() => {
        setIsFormulaMode = vi.fn();
        saveNullValue = vi.fn();
    });

    afterEach(() => {
        editor?.destroy();

        element?.remove();
    });

    describe('leaving formula mode', () => {
        it.each([
            ['Backspace', 8],
            ['Delete', 46],
        ])('leaves formula mode when %s lands on an empty editor', async (key, keyCode) => {
            await createEditor({options: {getIsFormulaMode: () => true, saveNullValue, setIsFormulaMode}});

            pressKey(key, keyCode);

            expect(setIsFormulaMode).toHaveBeenCalledWith(false);
            expect(saveNullValue).toHaveBeenCalled();
            expect(editor.storage.FormulaMode.isFormulaMode).toBe(false);
        });

        it('stays in formula mode while the editor still holds content', async () => {
            await createEditor({
                content: "<p>concat('a', 'b')</p>",
                options: {getIsFormulaMode: () => true, saveNullValue, setIsFormulaMode},
            });

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).not.toHaveBeenCalled();
            expect(saveNullValue).not.toHaveBeenCalled();
        });

        it('leaves formula mode when only whitespace is left', async () => {
            await createEditor({content: '<p>   </p>', options: {getIsFormulaMode: () => true, setIsFormulaMode}});

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).toHaveBeenCalledWith(false);
        });

        it('does nothing when the field was not in formula mode', async () => {
            await createEditor({options: {getIsFormulaMode: () => false, saveNullValue, setIsFormulaMode}});

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).not.toHaveBeenCalled();
            expect(saveNullValue).not.toHaveBeenCalled();
        });

        it('leaves formula mode without a save callback configured', async () => {
            await createEditor({options: {getIsFormulaMode: () => true, setIsFormulaMode}});

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).toHaveBeenCalledWith(false);
        });
    });

    describe('reading the current mode', () => {
        // Storage resets to its default when the extensions are rebuilt, so it cannot be the source of truth.
        it('prefers the getter over stale storage', async () => {
            await createEditor({options: {getIsFormulaMode: () => true, initialFormulaMode: false, setIsFormulaMode}});

            expect(editor.storage.FormulaMode.isFormulaMode).toBe(false);

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).toHaveBeenCalledWith(false);
        });

        it('does not exit on stale storage when the getter says the field left formula mode', async () => {
            await createEditor({options: {getIsFormulaMode: () => false, initialFormulaMode: true, setIsFormulaMode}});

            expect(editor.storage.FormulaMode.isFormulaMode).toBe(true);

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).not.toHaveBeenCalled();
        });

        it('falls back to storage without a getter', async () => {
            await createEditor({options: {initialFormulaMode: true, setIsFormulaMode}});

            pressKey('Backspace', 8);

            expect(setIsFormulaMode).toHaveBeenCalledWith(false);
        });
    });

    describe('storage', () => {
        it.each([
            [true, true],
            [false, false],
            [undefined, false],
        ])('initializes from initialFormulaMode %s', async (initialFormulaMode, expected) => {
            await createEditor({options: {initialFormulaMode, setIsFormulaMode}});

            expect(editor.storage.FormulaMode.isFormulaMode).toBe(expected);
        });

        it('follows the toggleFormulaMode command', async () => {
            await createEditor({options: {setIsFormulaMode}});

            editor.commands.toggleFormulaMode(true);

            expect(editor.storage.FormulaMode.isFormulaMode).toBe(true);

            editor.commands.toggleFormulaMode(false);

            expect(editor.storage.FormulaMode.isFormulaMode).toBe(false);
        });
    });
});
