import Document from '@tiptap/extension-document';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Text} from '@tiptap/extension-text';
import {Editor} from '@tiptap/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {MentionStorage} from '../MentionStorage.extension';
import {VariablePathSuggestion, VariablePathSuggestionPluginKey} from '../VariablePathSuggestion.extension';
import {getVariablePathSuggestionOptions} from '../variablePathSuggestionOptions';

vi.mock('tippy.js', () => ({default: vi.fn(() => [{popper: {style: {}}}])}));

const DATA_PILLS = [
    {id: 'attempts', value: 'attempts'},
    {id: 'judge.answers.question.isTrue', value: 'judge.answers.question.isTrue'},
];

describe('VariablePathSuggestion wiring', () => {
    let editor: Editor;

    beforeEach(() => {
        editor = new Editor({extensions: [Document, MentionStorage, Paragraph, Text, VariablePathSuggestion]});

        editor.storage.MentionStorage.dataPills = DATA_PILLS;
    });

    it('registers its suggestion plugin under its own key', () => {
        expect(VariablePathSuggestionPluginKey.getState(editor.state)).toBeDefined();
    });

    it('items() reads the data pills from storage and filters them by query', () => {
        const options = getVariablePathSuggestionOptions();

        const items = options.items!({editor, query: 'judge'} as never) as Array<{id: string}>;

        expect(items.map((item) => item.id)).toEqual(['judge.answers.question.isTrue']);
    });

    it('items() offers every data pill for an empty query', () => {
        const options = getVariablePathSuggestionOptions();

        expect(options.items!({editor, query: ''} as never)).toHaveLength(2);
    });

    it('items() offers nothing when no data pills are stored', () => {
        editor.storage.MentionStorage.dataPills = undefined as never;

        const options = getVariablePathSuggestionOptions();

        expect(options.items!({editor, query: ''} as never)).toEqual([]);
    });

    it('allow() is false when formula mode is not active', () => {
        const options = getVariablePathSuggestionOptions();

        expect(options.allow!({editor, isActive: true, range: {from: 0, to: 0}} as never)).toBe(false);
    });

    it('shouldShow() only activates the suggestion when a data pill matches the query', () => {
        const options = getVariablePathSuggestionOptions();

        expect(options.shouldShow!({editor, query: 'att'} as never)).toBe(true);
        expect(options.shouldShow!({editor, query: 'missing'} as never)).toBe(false);
    });

    it('command() replaces the typed partial path with the chosen path', () => {
        editor.commands.insertContent("exists('jud");

        const options = getVariablePathSuggestionOptions();

        options.command!({editor, props: {id: 'judge.answers.question.isTrue'}, range: {from: 9, to: 12}} as never);

        expect(editor.state.doc.textContent).toBe("exists('judge.answers.question.isTrue");
    });
});
