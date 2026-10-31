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
    getDisplayConditions: vi.fn(async () => ({})),
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

describe('getScriptCompletions for runtime action properties', () => {
    const properties = [
        {name: 'baseId', type: 'STRING'},
        {name: 'format', type: 'STRING'},
        {displayCondition: "format == 'xml'", name: 'xmlRoot', type: 'STRING'},
        {name: 'fields', propertiesDataSource: {propertiesLookupDependsOn: ['baseId']}, type: 'DYNAMIC_PROPERTIES'},
    ] as unknown as Property[];

    const runtimeCatalog: ComponentCatalogType = {
        ...catalog,
        components: [{name: 'airtable', title: 'Airtable', version: 1} as ComponentDefinitionBasic],
        getOperationProperties: vi.fn(async () => properties),
    };

    const base = 'function perform(input, connection, context) {\n  context.component.airtable.createRecord(';

    it('loads dynamic properties through the declared connection of the script node', async () => {
        const loadDynamicProperties = vi.fn(async () => [{name: 'Email', type: 'STRING'}] as unknown as Property[]);
        const context: ScriptCompletionContextI = {
            components: runtimeCatalog,
            connections: [{componentName: 'airtable', componentVersion: 1, name: 'at'}],
            input: {},
            kind: 'script',
            language: 'javascript',
            loadDynamicProperties,
            workflowNodeName: 'script_1',
        };
        const source = base + "{baseId: 'app1', fields: {";
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.label)).toEqual(['Email']);
        expect(loadDynamicProperties).toHaveBeenCalledWith({
            clusterElementNode: undefined,
            componentName: 'airtable',
            componentVersion: 1,
            connectionKey: 'at',
            lookupDependsOnPaths: ['baseId'],
            operationKind: 'action',
            operationName: 'createRecord',
            parameters: {baseId: 'app1'},
            propertyName: 'fields',
            workflowNodeName: 'script_1',
        });
    });

    it('hides a property whose display condition is false for the literal values written', async () => {
        const context: ScriptCompletionContextI = {
            components: runtimeCatalog,
            connections: [],
            input: {},
            kind: 'script',
            language: 'javascript',
        };
        const source = base + "{format: 'json', ";
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.label)).toEqual(['baseId', 'fields']);
    });

    it('passes the cluster element of a script inside a cluster root', async () => {
        const loadDynamicProperties = vi.fn(async () => [] as Property[]);
        const context: ScriptCompletionContextI = {
            clusterElementNode: {clusterElementType: 'tools', clusterElementWorkflowNodeName: 'script_1'},
            components: runtimeCatalog,
            connections: [{componentName: 'airtable', componentVersion: 1, name: 'at'}],
            input: {},
            kind: 'script',
            language: 'javascript',
            loadDynamicProperties,
            workflowNodeName: 'aiAgent_1',
        };
        const source = base + '{fields: {';

        await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(loadDynamicProperties).toHaveBeenCalledWith(
            expect.objectContaining({
                clusterElementNode: {clusterElementType: 'tools', clusterElementWorkflowNodeName: 'script_1'},
                workflowNodeName: 'aiAgent_1',
            })
        );
    });

    it('loads dynamic properties for the enclosing code workflow task and its declared connection', async () => {
        const loadDynamicProperties = vi.fn(async () => [{name: 'Email', type: 'STRING'}] as unknown as Property[]);
        const context: CodeWorkflowCompletionContextI = {
            components: runtimeCatalog,
            kind: 'codeWorkflow',
            language: 'javascript',
            loadDynamicProperties,
        };
        const source =
            '({workflows: [{name: "w", tasks: [{name: "createRow", connections: [{componentName: "airtable", componentVersion: 1, name: "at"}], perform: function (context) {\ncontext.component.airtable.createRecord({baseId: "app1", fields: {';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});

        expect(items.map((item) => item.label)).toEqual(['Email']);
        expect(loadDynamicProperties).toHaveBeenCalledWith(
            expect.objectContaining({
                connectionKey: 'at',
                parameters: {baseId: 'app1'},
                propertyName: 'fields',
                workflowNodeName: 'createRow',
            })
        );
    });

    it('offers no dynamic properties without a connection declared for the component', async () => {
        const loadDynamicProperties = vi.fn();
        const context: ScriptCompletionContextI = {
            components: runtimeCatalog,
            connections: [],
            input: {},
            kind: 'script',
            language: 'javascript',
            loadDynamicProperties,
            workflowNodeName: 'script_1',
        };
        const source = base + '{fields: {';

        expect(await getScriptCompletions({context, cursorOffset: source.length, source})).toEqual([]);
        expect(loadDynamicProperties).not.toHaveBeenCalled();
    });
});

describe('getScriptCompletions for action property values', () => {
    const properties = [
        {name: 'method', options: [{value: 'GET'}, {value: "it's"}], type: 'STRING'},
        {name: 'followRedirect', type: 'BOOLEAN'},
        {name: 'tableId', optionsDataSource: {optionsLookupDependsOn: []}, type: 'STRING'},
    ] as unknown as Property[];

    const valueCatalog: ComponentCatalogType = {
        ...catalog,
        getOperationProperties: vi.fn(async () => properties),
    };

    const contextFor = (language: 'javascript' | 'python', extra: Partial<ScriptCompletionContextI> = {}) =>
        ({
            components: valueCatalog,
            connections: [{componentName: 'airtable', componentVersion: 1, name: 'at'}],
            input: {},
            kind: 'script',
            language,
            ...extra,
        }) as ScriptCompletionContextI;

    it('inserts quoted string literals after a key', async () => {
        const source = 'function perform(input, connection, context) {\n  context.component.airtable.list({method: G';
        const items = await getScriptCompletions({
            context: contextFor('javascript'),
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => [item.insertText, item.replaceLength])).toEqual([
            ["'GET'", 1],
            ["'it\\'s'", 1],
        ]);
    });

    it('inserts raw text inside an open quote and Python booleans outside one', async () => {
        const quoted = 'def perform(input, connection, context):\n    context.component.airtable.list({"method": "G';
        const quotedItems = await getScriptCompletions({
            context: contextFor('python'),
            cursorOffset: quoted.length,
            source: quoted,
        });

        expect(quotedItems.map((item) => item.insertText)).toEqual(['GET', "it's"]);

        const flag =
            'def perform(input, connection, context):\n    context.component.airtable.list({"followRedirect": ';
        const flagItems = await getScriptCompletions({
            context: contextFor('python'),
            cursorOffset: flag.length,
            source: flag,
        });

        expect(flagItems.map((item) => item.insertText)).toEqual(['True', 'False']);
    });

    it('loads options through the declared connection of the script node', async () => {
        const loadOptions = vi.fn(async () => [{label: 'Customers', value: 'tbl1'}]);
        const source = 'function perform(input, connection, context) {\n  context.component.airtable.list({tableId: ';
        const items = await getScriptCompletions({
            context: contextFor('javascript', {loadOptions, workflowNodeName: 'script_1'}),
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => [item.label, item.insertText])).toEqual([['Customers', "'tbl1'"]]);
        expect(loadOptions).toHaveBeenCalledWith(
            expect.objectContaining({connectionKey: 'at', propertyName: 'tableId', workflowNodeName: 'script_1'})
        );
    });
});

describe('getScriptCompletions connection choice', () => {
    const optionsCatalog: ComponentCatalogType = {
        ...catalog,
        getOperationProperties: vi.fn(
            async () => [{name: 'channel', optionsDataSource: {}, type: 'STRING'}] as unknown as Property[]
        ),
    };

    const contextWith = (loadOptions: ScriptCompletionContextI['loadOptions']): ScriptCompletionContextI => ({
        components: optionsCatalog,
        connections: [
            {componentName: 'slack', componentVersion: 1, name: 'slack_work'},
            {componentName: 'slack', componentVersion: 1, name: 'slack_personal'},
        ],
        input: {},
        kind: 'script',
        language: 'javascript',
        loadOptions,
        workflowNodeName: 'script_1',
    });

    it('uses the connection named in the call', async () => {
        const loadOptions = vi.fn(async () => []);
        const before = 'function perform(input, connection, context) {\n  context.component.slack.send({channel: ';
        const source = `${before}}, 'slack_personal')\n}`;

        await getScriptCompletions({context: contextWith(loadOptions), cursorOffset: before.length, source});

        expect(loadOptions).toHaveBeenCalledWith(expect.objectContaining({connectionKey: 'slack_personal'}));
    });

    it('sends no connection when several match and the call names none', async () => {
        const loadOptions = vi.fn(async () => []);
        const source = 'function perform(input, connection, context) {\n  context.component.slack.send({channel: ';

        await getScriptCompletions({context: contextWith(loadOptions), cursorOffset: source.length, source});

        expect(loadOptions).toHaveBeenCalledWith(expect.objectContaining({connectionKey: undefined}));
    });
});
