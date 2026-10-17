import {describe, expect, it, vi} from 'vitest';

import {registerCompletionContext} from './completionContextRegistry';
import {buildCompletionProvider, registerCompletionProviders} from './registerCompletionProviders';

import type {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

const registerCompletionItemProvider = vi.fn(() => ({dispose: vi.fn()}));

const fakeMonaco = {
    languages: {
        CompletionItemInsertTextRule: {InsertAsSnippet: 4},
        CompletionItemKind: {Function: 1, Method: 0, Module: 8, Property: 9, Value: 12, Variable: 4},
        registerCompletionItemProvider,
    },
} as unknown as typeof import('monaco-editor');

describe('registerCompletionProviders', () => {
    it('registers one provider per language and only once', () => {
        registerCompletionProviders(fakeMonaco);
        registerCompletionProviders(fakeMonaco);

        expect(registerCompletionItemProvider).toHaveBeenCalledTimes(5);
        expect(registerCompletionItemProvider.mock.calls.map((call) => (call as unknown[])[0])).toEqual([
            'javascript',
            'python',
            'ruby',
            'json',
            'yaml',
        ]);
    });
});

describe('buildCompletionProvider', () => {
    const model = (value: string, uri: string) => ({
        getOffsetAt: () => value.length,
        getValue: () => value,
        getWordUntilPosition: () => ({endColumn: 1, startColumn: 1}),
        uri: {toString: () => uri},
    });
    const position = {column: 1, lineNumber: 1};

    it('returns no suggestions for a model without a context', async () => {
        const provider = buildCompletionProvider(fakeMonaco);

        expect(
            await provider.provideCompletionItems(
                model('context.', 'inmemory://none') as never,
                position as never,
                {} as never,
                {} as never
            )
        ).toEqual({suggestions: []});
    });

    it('returns script suggestions for a registered model', async () => {
        registerCompletionContext('inmemory://script', {kind: 'customComponent', language: 'javascript'});

        const provider = buildCompletionProvider(fakeMonaco);

        const result = await provider.provideCompletionItems(
            model(
                'actions: [{name: "a", perform: function (input, connection, context) {\ncontext.',
                'inmemory://script'
            ) as never,
            position as never,
            {} as never,
            {} as never
        );

        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['http', 'log']);
    });

    it('gives every suggestion a sort text below the TypeScript service range', async () => {
        registerCompletionContext('inmemory://sorted', {kind: 'customComponent', language: 'javascript'});

        const provider = buildCompletionProvider(fakeMonaco);

        const result = await provider.provideCompletionItems(
            model(
                'actions: [{name: "a", perform: function (input, connection, context) {\ncontext.',
                'inmemory://sorted'
            ) as never,
            position as never,
            {} as never,
            {} as never
        );

        expect(result?.suggestions.map((suggestion) => suggestion.sortText)).toEqual(['0http', '0log']);
        expect(result?.suggestions.every((suggestion) => (suggestion.sortText ?? '') < '10')).toBe(true);
    });

    it('logs and returns no suggestions when the completion lookup fails', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        registerCompletionContext('inmemory://failing', {
            components: {
                components: [{name: 'x', title: 'X', version: 1} as ComponentDefinitionBasic],
                getConnectionDefinition: vi.fn().mockRejectedValue(new Error('boom')),
                getDefinition: vi.fn().mockRejectedValue(new Error('boom')),
            },
            connections: [],
            input: {},
            kind: 'script',
            language: 'javascript',
        });

        const provider = buildCompletionProvider(fakeMonaco);

        const result = await provider.provideCompletionItems(
            model(
                'function perform(input, connection, context) {\ncontext.component.x.',
                'inmemory://failing'
            ) as never,
            position as never,
            {} as never,
            {} as never
        );

        expect(result).toEqual({suggestions: []});
        expect(consoleErrorSpy).toHaveBeenCalledTimes(1);

        consoleErrorSpy.mockRestore();
    });
});
