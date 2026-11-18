import {describe, expect, it, vi} from 'vitest';

import {registerCompletionContext} from './completionContextRegistry';
import {
    buildCompletionProvider,
    buildJavaScriptCompletionProvider,
    registerCompletionProviders,
} from './registerCompletionProviders';

import type {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import type {
    ComponentDefinition,
    ComponentDefinitionBasic,
    Property,
    WorkflowNodeOutput,
} from '@/shared/middleware/platform/configuration';

const registerCompletionItemProvider = vi.fn(() => ({dispose: vi.fn()}));
const setModeConfiguration = vi.fn();
const getCompletionsAtPosition = vi.fn();

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
        CompletionItemInsertTextRule: {InsertAsSnippet: 4},
        CompletionItemKind: {Function: 1, Method: 0, Module: 8, Property: 9, Value: 12, Variable: 4},
        CompletionItemTag: {Deprecated: 1},
        CompletionTriggerKind: {Invoke: 0, TriggerCharacter: 1},
        registerCompletionItemProvider,
    },
    typescript: {
        getJavaScriptWorker: async () => async () => ({getCompletionsAtPosition}),
        javascriptDefaults: {modeConfiguration: {completionItems: true, hovers: true}, setModeConfiguration},
    },
} as unknown as typeof import('monaco-editor');

describe('registerCompletionProviders', () => {
    it('registers one provider per language and only once', () => {
        registerCompletionProviders(fakeMonaco);
        registerCompletionProviders(fakeMonaco);

        expect(registerCompletionItemProvider).toHaveBeenCalledTimes(6);
        expect(registerCompletionItemProvider.mock.calls.map((call) => (call as unknown[])[0])).toEqual([
            'javascript',
            'python',
            'ruby',
            'shell',
            'json',
            'yaml',
        ]);
        expect(setModeConfiguration).toHaveBeenCalledWith({completionItems: false, hovers: true});
    });
});

describe('buildJavaScriptCompletionProvider', () => {
    const uri = 'inmemory://javascript-script';
    const head = 'function perform(input, connection, context) {\n';

    registerCompletionContext(uri, {
        components: {
            components: [{name: 'googleMaps', title: 'Google Maps', version: 1} as ComponentDefinitionBasic],
            getConnectionDefinition: vi.fn(),
            getDefinition: vi.fn(),
            getDisplayConditions: vi.fn(async () => ({})),
            getOperationProperties: vi.fn(async () => [{name: 'latitude', type: 'NUMBER'}] as unknown as Property[]),
        },
        connections: [],
        input: {},
        kind: 'script',
        language: 'javascript',
    });

    const javaScriptModel = (value: string) => ({
        getLineContent: () => value.split('\n').at(-1) ?? '',
        getOffsetAt: () => value.length,
        getValue: () => value,
        getWordUntilPosition: () => ({endColumn: 1, startColumn: 1}),
        isDisposed: () => false,
        uri: {toString: () => uri},
    });

    const provide = (value: string, completionContext: object) =>
        buildJavaScriptCompletionProvider(fakeMonaco).provideCompletionItems(
            javaScriptModel(value) as never,
            {column: 1, lineNumber: 1} as never,
            completionContext as never,
            {} as never
        );

    getCompletionsAtPosition.mockResolvedValue({
        entries: [{kind: 'var', name: 'AbortController', sortText: '15'}],
    });

    it('offers only the action property keys inside the parameters of an action call', async () => {
        const result = await provide(`${head}context.component.googleMaps.getAddress({`, {
            triggerCharacter: '{',
            triggerKind: 1,
        });

        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['latitude']);
    });

    it('drops the language service items when invoked explicitly in a property key position', async () => {
        const result = await provide(`${head}context.component.googleMaps.getAddress({lat`, {triggerKind: 0});

        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['latitude']);
    });

    it('keeps the language service items in ordinary code', async () => {
        const result = await provide(`${head}const controller = new Abo`, {triggerKind: 0});

        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['AbortController']);
    });

    it('does not ask the language service on a trigger character it does not handle', async () => {
        getCompletionsAtPosition.mockClear();

        const result = await provide(`${head}const text = '`, {triggerCharacter: "'", triggerKind: 1});

        expect(result?.suggestions).toEqual([]);
        expect(getCompletionsAtPosition).not.toHaveBeenCalled();
    });

    it('returns the language service items for an editor without a ByteChef context', async () => {
        const result = await buildJavaScriptCompletionProvider(fakeMonaco).provideCompletionItems(
            {...javaScriptModel('Abo'), uri: {toString: () => 'inmemory://plain'}} as never,
            {column: 4, lineNumber: 1} as never,
            {triggerKind: 0} as never,
            {} as never
        );

        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['AbortController']);
    });
});

describe('buildCompletionProvider', () => {
    const model = (value: string, uri: string) => ({
        getLineContent: () => value.split('\n').at(-1) ?? '',
        getOffsetAt: () => value.length,
        getValue: () => value,
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

    it('returns shell suggestions for a model registered as a shell script', async () => {
        registerCompletionContext('inmemory://shell', {env: ['API_TOKEN'], input: {}, kind: 'shell'});

        const provider = buildCompletionProvider(fakeMonaco);

        const result = await provider.provideCompletionItems(
            model('echo $API', 'inmemory://shell') as never,
            {column: 10, lineNumber: 1} as never,
            {} as never,
            {} as never
        );

        const suggestion = result?.suggestions.find((candidate) => candidate.label === 'API_TOKEN');

        expect(suggestion?.insertText).toBe('API_TOKEN');
        expect(suggestion?.range).toMatchObject({endColumn: 10, startColumn: 7});
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
                getDisplayConditions: vi.fn(async () => ({})),
                getOperationProperties: vi.fn(async () => []),
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

    describe('replace range in a JSON workflow definition', () => {
        const uri = 'inmemory://workflow-definition';

        registerCompletionContext(uri, {
            components: {
                components: [{name: 'googleMail', title: 'Gmail', version: 1} as ComponentDefinitionBasic],
                getConnectionDefinition: vi.fn(),
                getDefinition: vi.fn(
                    async () =>
                        ({actions: [{name: 'sendEmail', title: 'Send Email'}]}) as unknown as ComponentDefinition
                ),
                getDisplayConditions: vi.fn(async () => ({})),
                getOperationProperties: vi.fn(async () => []),
            },
            format: 'json',
            functions: [
                {
                    description: 'Joins',
                    name: 'join',
                    parameters: [],
                    title: 'Join',
                } as unknown as EvaluatorFunctionDefinition,
            ],
            getNodeOutputs: vi.fn(async () => [
                {
                    outputResponse: {
                        outputSchema: {properties: [{name: 'body', type: 'OBJECT'}], type: 'OBJECT'},
                    },
                    workflowNodeName: 'trigger_1',
                } as unknown as WorkflowNodeOutput,
            ]),
            kind: 'workflowDefinition',
        });

        const provideAt = async (line: string, suffix: string) => {
            const prefix = '{\n  "triggers": [{"name": "trigger_1"}],\n  "tasks": [{"name": "t1", ';
            const source = `${prefix}\n${line}${suffix}`;

            const jsonModel = {
                getLineContent: (lineNumber: number) => source.split('\n')[lineNumber - 1],
                getOffsetAt: () => prefix.length + 1 + line.length,
                getValue: () => source,
                uri: {toString: () => uri},
            };

            return buildCompletionProvider(fakeMonaco).provideCompletionItems(
                jsonModel as never,
                {column: line.length + 1, lineNumber: 4} as never,
                {} as never,
                {} as never
            );
        };

        it('covers only the function name after =', async () => {
            const line = '"parameters": {"x": "=jo';
            const result = await provideAt(line, '"}}]}');

            expect(result?.suggestions[0]).toMatchObject({
                filterText: 'join',
                range: {endColumn: line.length + 1, startColumn: line.length - 1},
            });
        });

        it('inserts a property at the cursor after the dot', async () => {
            const line = '"parameters": {"x": "${trigger_1.';
            const result = await provideAt(line, '}"}}]}');

            expect(result?.suggestions[0]).toMatchObject({
                label: 'body',
                range: {endColumn: line.length + 1, startColumn: line.length + 1},
            });
        });

        it('keeps the ${ out of the range', async () => {
            const line = '"parameters": {"x": "${tri';
            const result = await provideAt(line, '}"}}]}');

            expect(result?.suggestions[0]).toMatchObject({
                label: 'trigger_1',
                range: {endColumn: line.length + 1, startColumn: line.length - 2},
            });
        });

        it('inserts an action at the cursor after the version slash with the bare name as filter text', async () => {
            const line = '"type": "googleMail/v1/';
            const result = await provideAt(line, '"}]}');

            expect(result?.suggestions[0]).toMatchObject({
                filterText: 'sendEmail',
                insertText: 'sendEmail',
                range: {endColumn: line.length + 1, startColumn: line.length + 1},
            });
        });
    });

    describe('replace range for an option value', () => {
        const uri = 'inmemory://workflow-definition-values';

        registerCompletionContext(uri, {
            components: {
                components: [],
                getConnectionDefinition: vi.fn(),
                getDefinition: vi.fn(),
                getDisplayConditions: vi.fn(async () => ({})),
                getOperationProperties: vi.fn(
                    async () =>
                        [
                            {name: 'mimeType', options: [{value: 'application/json'}], type: 'STRING'},
                        ] as unknown as Property[]
                ),
            },
            format: 'json',
            functions: [],
            getNodeOutputs: vi.fn(async () => []),
            kind: 'workflowDefinition',
        });

        it('replaces everything typed of the value, not only the trailing word', async () => {
            const line =
                '{"tasks": [{"name": "h", "type": "httpClient/v1/get", "parameters": {"mimeType": "application/js';
            const source = `${line}"}}]}`;
            const model = {
                getLineContent: () => source,
                getOffsetAt: () => line.length,
                getValue: () => source,
                uri: {toString: () => uri},
            };

            const result = await buildCompletionProvider(fakeMonaco).provideCompletionItems(
                model as never,
                {column: line.length + 1, lineNumber: 1} as never,
                {} as never,
                {} as never
            );

            expect(result?.suggestions[0]).toMatchObject({
                insertText: 'application/json',
                range: {endColumn: line.length + 1, startColumn: line.length + 1 - 'application/js'.length},
            });
        });
    });
});
