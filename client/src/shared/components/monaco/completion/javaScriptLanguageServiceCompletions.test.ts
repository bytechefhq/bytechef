import {describe, expect, it, vi} from 'vitest';

import {
    getLanguageServiceCompletionItems,
    isLanguageServiceCompletionItem,
    resolveLanguageServiceCompletionItem,
    toLanguageServiceCompletionItemKind,
} from './javaScriptLanguageServiceCompletions';

const getCompletionsAtPosition = vi.fn();
const getCompletionEntryDetails = vi.fn();

const fakeMonaco = {
    Range: class {
        constructor(
            public startLineNumber: number,
            public startColumn: number,
            public endLineNumber: number,
            public endColumn: number
        ) {}
    },
    languages: {
        CompletionItemKind: {
            Class: 5,
            Enum: 15,
            Field: 3,
            File: 20,
            Function: 1,
            Interface: 7,
            Keyword: 17,
            Module: 8,
            Property: 9,
            Variable: 4,
        },
        CompletionItemTag: {Deprecated: 1},
    },
    typescript: {
        getJavaScriptWorker: async () => async () => ({getCompletionEntryDetails, getCompletionsAtPosition}),
    },
} as unknown as typeof import('monaco-editor');

const uri = {toString: () => 'inmemory://script'};

const model = {
    getOffsetAt: () => 10,
    getPositionAt: (offset: number) => ({column: offset + 1, lineNumber: 1}),
    getWordUntilPosition: () => ({endColumn: 11, startColumn: 8}),
    isDisposed: () => false,
    uri,
};

describe('toLanguageServiceCompletionItemKind', () => {
    it('maps the TypeScript element kinds the way the Monaco TypeScript mode does', () => {
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'keyword')).toBe(17);
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'local var')).toBe(4);
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'getter')).toBe(3);
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'method')).toBe(1);
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'class')).toBe(5);
        expect(toLanguageServiceCompletionItemKind(fakeMonaco, 'parameter')).toBe(9);
    });
});

describe('getLanguageServiceCompletionItems', () => {
    it('converts the worker entries to completion items over the word or the replacement span', async () => {
        getCompletionsAtPosition.mockResolvedValue({
            entries: [
                {kind: 'var', name: 'AbortController', sortText: '15'},
                {
                    kind: 'method',
                    kindModifiers: 'deprecated',
                    name: 'escape',
                    replacementSpan: {length: 2, start: 4},
                    sortText: '11',
                },
            ],
        });

        const items = await getLanguageServiceCompletionItems(
            fakeMonaco,
            model as never,
            {column: 11, lineNumber: 1} as never
        );

        expect(getCompletionsAtPosition).toHaveBeenCalledWith('inmemory://script', 10);
        expect(items).toMatchObject([
            {
                insertText: 'AbortController',
                kind: 4,
                label: 'AbortController',
                languageServiceOffset: 10,
                range: {endColumn: 11, startColumn: 8},
                sortText: '15',
                tags: [],
            },
            {kind: 1, label: 'escape', range: {endColumn: 7, startColumn: 5}, tags: [1]},
        ]);
        expect(items.every(isLanguageServiceCompletionItem)).toBe(true);
    });

    it('returns nothing when the worker has no completions', async () => {
        getCompletionsAtPosition.mockResolvedValue(undefined);

        expect(
            await getLanguageServiceCompletionItems(fakeMonaco, model as never, {column: 11, lineNumber: 1} as never)
        ).toEqual([]);
    });
});

describe('resolveLanguageServiceCompletionItem', () => {
    const item = {
        insertText: 'escape',
        kind: 1,
        label: 'escape',
        languageServiceOffset: 10,
        languageServiceUri: uri,
        range: {endColumn: 7, endLineNumber: 1, startColumn: 5, startLineNumber: 1},
    };

    it('adds the signature and documentation from the worker', async () => {
        getCompletionEntryDetails.mockResolvedValue({
            displayParts: [{text: 'function '}, {text: 'escape(string: string): string'}],
            documentation: [{text: 'Encodes a string.'}],
            kind: 'function',
            name: 'escape',
        });

        expect(await resolveLanguageServiceCompletionItem(fakeMonaco, item as never)).toMatchObject({
            detail: 'function escape(string: string): string',
            documentation: {value: 'Encodes a string.'},
            label: 'escape',
        });
        expect(getCompletionEntryDetails).toHaveBeenCalledWith('inmemory://script', 10, 'escape');
    });

    it('keeps the item when the worker has no details', async () => {
        getCompletionEntryDetails.mockResolvedValue(undefined);

        expect(await resolveLanguageServiceCompletionItem(fakeMonaco, item as never)).toBe(item);
    });
});
