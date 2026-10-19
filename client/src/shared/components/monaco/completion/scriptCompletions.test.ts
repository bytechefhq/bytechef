import {describe, expect, it, vi} from 'vitest';

import {getScriptCompletions} from './scriptCompletions';

import type {
    ComponentDefinition,
    ComponentDefinitionBasic,
    ConnectionDefinition,
    Property,
} from '@/shared/middleware/platform/configuration';

import type {
    CodeWorkflowCompletionContextI,
    ComponentCatalogType,
    CustomComponentCompletionContextI,
    ScriptCompletionContextI,
} from './completionTypes';

const catalog: ComponentCatalogType = {
    components: [
        {name: 'googleMaps', title: 'Google Maps', version: 1} as ComponentDefinitionBasic,
        {name: 'logger', title: 'Logger', version: 1} as ComponentDefinitionBasic,
        {name: 'aiAgent', title: 'AI Agent', version: 1} as ComponentDefinitionBasic,
    ],
    getConnectionDefinition: vi.fn(
        async () => ({properties: [{name: 'apiKey'}, {name: 'region'}]}) as unknown as ConnectionDefinition
    ),
    getDefinition: vi.fn(
        async (name: string) =>
            ({
                actionClusterElementTypes: name === 'aiAgent' ? {chat: ['MODEL', 'TOOLS']} : {},
                actions: [{name: name === 'aiAgent' ? 'chat' : 'getAddress', title: 'Get Address'}],
                name,
                version: 1,
            }) as unknown as ComponentDefinition
    ),
    getOperationProperties: vi.fn(
        async () =>
            [
                {name: 'latitude', required: true, type: 'NUMBER'},
                {name: 'longitude', required: true, type: 'NUMBER'},
                {name: 'options', properties: [{name: 'language', type: 'STRING'}], type: 'OBJECT'},
            ] as unknown as Property[]
    ),
};

describe('getScriptCompletions for a script', () => {
    const context: ScriptCompletionContextI = {
        components: catalog,
        connections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}],
        input: {order: {id: 1}, total: 5},
        kind: 'script',
        language: 'javascript',
    };
    const base = 'function perform(input, connection, context) {\n';

    it('offers only component on context', async () => {
        const source = base + 'context.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.label)).toEqual(['component']);
    });

    it('sorts connected components first and marks them', async () => {
        const source = base + 'context.component.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.insertText)).toEqual(['googleMaps', 'logger', 'aiAgent']);
        expect(items[0].detail).toBe('connected');
        expect(items.map((item) => item.sortText)).toEqual(['0_googleMaps', '1_logger', '1_aiAgent']);
    });

    it('offers actions with the single connection prefilled', async () => {
        const source = base + 'context.component.googleMaps.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items[0]).toMatchObject({
            insertAsSnippet: true,
            insertText: "getAddress({$1}, 'gm')",
            label: 'getAddress',
        });
    });

    it('does not offer cluster elements in a script', async () => {
        const source = base + 'context.component.aiAgent.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items[0].insertText).toBe('chat({$1})');
    });

    it('offers input keys and nested keys', async () => {
        expect(
            (await getScriptCompletions({context, cursorOffset: base.length + 6, source: base + 'input.'})).map(
                (item) => item.label
            )
        ).toEqual(['order', 'total']);
        expect(
            (await getScriptCompletions({context, cursorOffset: base.length + 12, source: base + 'input.order.'})).map(
                (item) => item.label
            )
        ).toEqual(['id']);
    });

    it("offers connection names then that connection's properties, for dot and bracket access", async () => {
        expect(
            (await getScriptCompletions({context, cursorOffset: base.length + 11, source: base + 'connection.'})).map(
                (item) => item.label
            )
        ).toEqual(['gm']);
        expect(
            (
                await getScriptCompletions({context, cursorOffset: base.length + 14, source: base + 'connection.gm.'})
            ).map((item) => item.label)
        ).toEqual(['apiKey', 'region']);
        expect(
            (
                await getScriptCompletions({
                    context,
                    cursorOffset: base.length + 17,
                    source: base + "connection['gm'].",
                })
            ).map((item) => item.label)
        ).toEqual(['apiKey', 'region']);
    });

    it('adds authorization properties after connection properties without duplicates', async () => {
        const authorizationCatalog: ComponentCatalogType = {
            ...catalog,
            getConnectionDefinition: vi.fn(
                async () =>
                    ({
                        authorizations: [{name: 'oauth2', properties: [{name: 'clientId'}, {name: 'apiKey'}]}],
                        properties: [{name: 'apiKey'}, {name: 'region'}],
                    }) as unknown as ConnectionDefinition
            ),
        };
        const source = base + 'connection.gm.';
        const items = await getScriptCompletions({
            context: {...context, components: authorizationCatalog},
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['apiKey', 'region', 'clientId']);
    });

    it('returns nothing outside perform', async () => {
        expect(await getScriptCompletions({context, cursorOffset: 8, source: 'context.'})).toEqual([]);
    });

    it('offers no context members on a callback parameter', async () => {
        const source = base + 'return input.rows.map((row) => row.';

        expect(await getScriptCompletions({context, cursorOffset: source.length, source})).toEqual([]);
    });

    it('offers components under context inside a block-bodied callback', async () => {
        const source = base + 'input.rows.map((row) => {\n  context.component.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.insertText)).toEqual(['googleMaps', 'logger', 'aiAgent']);
    });

    it('leaves bare roots to the TypeScript service in JavaScript', async () => {
        const source = base + 'con';

        expect(await getScriptCompletions({context, cursorOffset: source.length, source})).toEqual([]);
    });

    it('offers the perform parameters as bare roots in Python', async () => {
        const pythonContext: ScriptCompletionContextI = {...context, language: 'python'};
        const source = 'def perform(input, connection, context):\n    con';

        expect(
            (await getScriptCompletions({context: pythonContext, cursorOffset: source.length, source})).map(
                (item) => item.label
            )
        ).toEqual(['input', 'connection', 'context']);
    });

    it('offers input keys inside a callback', async () => {
        const source = base + 'list.forEach((row) => {\n  input.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.label)).toEqual(['order', 'total']);
    });
});

describe('getScriptCompletions for a custom component', () => {
    const context: CustomComponentCompletionContextI = {kind: 'customComponent', language: 'javascript'};

    it('offers http and log on context and the enclosing action properties on input', async () => {
        const source =
            '({connection: {properties: [{name: "token"}]}, actions: [{name: "a", properties: [{name: "q"}], perform: function (input, connection, context) {\ncontext.';

        expect(
            (await getScriptCompletions({context, cursorOffset: source.length, source})).map((item) => item.label)
        ).toEqual(['http', 'log']);

        const inputSource = source.replace(/context\.$/, 'input.');

        expect(
            (await getScriptCompletions({context, cursorOffset: inputSource.length, source: inputSource})).map(
                (item) => item.label
            )
        ).toEqual(['q']);

        const connectionSource = source.replace(/context\.$/, 'connection.');

        expect(
            (
                await getScriptCompletions({context, cursorOffset: connectionSource.length, source: connectionSource})
            ).map((item) => item.label)
        ).toEqual(['token']);
    });
});

describe('getScriptCompletions for a code workflow', () => {
    const context: CodeWorkflowCompletionContextI = {components: catalog, kind: 'codeWorkflow', language: 'javascript'};
    const source =
        '({workflows: [{name: "w", inputs: [{name: "orderId"}], tasks: [{name: "fetch", perform: (context) => 1}, {name: "n", connections: [{componentName: "aiAgent", name: "agent"}], perform: function (context) {\ncontext.';

    it('offers the five runtime members', async () => {
        expect(
            (await getScriptCompletions({context, cursorOffset: source.length, source})).map((item) => item.label)
        ).toEqual(['component', 'connection', 'input', 'log', 'parameters']);
    });

    it('places the caret after the parentheses of zero-argument members', async () => {
        expect(
            (await getScriptCompletions({context, cursorOffset: source.length, source})).map((item) => item.insertText)
        ).toEqual(['component', 'connection()$0', 'input()$0', 'log($1)', 'parameters()$0']);
    });

    it('offers inputs and earlier tasks under context.input()', async () => {
        const inputSource = source + 'input().';

        expect(
            (await getScriptCompletions({context, cursorOffset: inputSource.length, source: inputSource})).map(
                (item) => item.label
            )
        ).toEqual(['orderId', 'fetch']);
    });

    it('adds the cluster elements stop for an action that accepts them', async () => {
        const actionSource = source + 'component.aiAgent.';

        expect(
            (await getScriptCompletions({context, cursorOffset: actionSource.length, source: actionSource}))[0]
                .insertText
        ).toBe("chat({$1}, 'agent', {$3})");
    });

    it('resolves a Python task through its named function', async () => {
        const pythonContext: CodeWorkflowCompletionContextI = {...context, language: 'python'};
        const pythonSource = 'def greet(context):\n    context.input().';
        const fullSource =
            pythonSource +
            '\n\nproject = {"workflows": [{"name": "w", "inputs": [{"name": "name"}], "tasks": [{"name": "greet", "perform": greet}]}]}';

        expect(
            (
                await getScriptCompletions({
                    context: pythonContext,
                    cursorOffset: pythonSource.length,
                    source: fullSource,
                })
            ).map((item) => item.label)
        ).toEqual(['name']);
    });
});

describe('action property completions', () => {
    const scriptContext: ScriptCompletionContextI = {
        components: catalog,
        connections: [{componentName: 'googleMaps', componentVersion: 2, name: 'gm'}],
        input: {},
        kind: 'script',
        language: 'javascript',
    };
    const head = 'function perform(input, connection, context) {\n';
    const labels = async (
        context: ScriptCompletionContextI | CodeWorkflowCompletionContextI | CustomComponentCompletionContextI,
        source: string,
        cursorOffset = source.length
    ) => (await getScriptCompletions({context, cursorOffset, source})).map((item) => item.label);

    it('offers the action properties in the first argument with required ones first', async () => {
        expect(await labels(scriptContext, `${head}context.component.googleMaps.getAddress({lat`)).toEqual([
            'latitude',
            'longitude',
            'options',
        ]);
        expect(catalog.getOperationProperties).toHaveBeenCalledWith('googleMaps', 2, 'getAddress', 'action');
    });

    it('leaves out keys already written and walks into nested objects', async () => {
        expect(await labels(scriptContext, `${head}context.component.googleMaps.getAddress({latitude: 1, `)).toEqual([
            'longitude',
            'options',
        ]);
        expect(await labels(scriptContext, `${head}context.component.googleMaps.getAddress({options: {`)).toEqual([
            'language',
        ]);
    });

    it('offers properties inside a Python dict key in a code workflow function', async () => {
        const codeWorkflowContext: CodeWorkflowCompletionContextI = {
            components: catalog,
            kind: 'codeWorkflow',
            language: 'python',
        };
        const before = 'def greet(context):\n    return context.component.googleMaps.getAddress({"';
        const source = `${before}"})\n\nproject = {"workflows": [{"name": "w", "tasks": [{"name": "greet", "perform": greet}]}]}`;

        expect(await labels(codeWorkflowContext, source, before.length)).toEqual(['latitude', 'longitude', 'options']);
    });

    it('offers nothing for a call that is not on the perform context or in a custom component', async () => {
        expect(await labels(scriptContext, `${head}other.component.googleMaps.getAddress({`)).toEqual([]);
        expect(
            await labels(
                {kind: 'customComponent', language: 'javascript'},
                'actions: [{name: "a", perform: function (input, connection, context) {\ncontext.component.googleMaps.getAddress({'
            )
        ).toEqual([]);
    });
});
