import {describe, expect, it, vi} from 'vitest';

import {getWorkflowDefinitionCompletions} from './workflowDefinitionCompletions';

import type {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import type {
    ComponentDefinition,
    ComponentDefinitionBasic,
    ConnectionDefinition,
    Property,
    WorkflowNodeOutput,
} from '@/shared/middleware/platform/configuration';

import type {ComponentCatalogType, WorkflowDefinitionCompletionContextI} from './completionTypes';

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
                actions: [{name: name === 'aiAgent' ? 'chat' : 'getAddress', title: 'Get Address'}],
                name,
                version: 1,
            }) as unknown as ComponentDefinition
    ),
    getDisplayConditions: vi.fn(async () => ({})),
    getOperationProperties: vi.fn(
        async (_componentName: string, _componentVersion: number | undefined, _operationName: string, kind: string) =>
            (kind === 'trigger'
                ? [{name: 'channel', required: true, type: 'STRING'}]
                : [
                      {name: 'uri', required: true, type: 'STRING'},
                      {name: 'headers', properties: [{name: 'accept', type: 'STRING'}], type: 'OBJECT'},
                  ]) as unknown as Property[]
    ),
};

const functions: EvaluatorFunctionDefinition[] = [
    {
        category: 'STRING',
        description: 'Joins',
        example: '',
        name: 'join',
        parameters: [
            {description: '', name: 'separator', required: true, type: 'STRING'},
            {description: '', name: 'list', required: true, type: 'LIST'},
        ],
        returnType: 'STRING',
        title: 'Join',
    },
    {
        category: 'DATE_TIME',
        description: 'Now',
        example: '',
        name: 'now',
        parameters: [],
        returnType: 'DATETIME',
        title: 'Now',
    },
] as EvaluatorFunctionDefinition[];

const nodeOutputs = [
    {
        outputResponse: {
            outputSchema: {
                properties: [{name: 'body', properties: [{name: 'amount', type: 'NUMBER'}], type: 'OBJECT'}],
                type: 'OBJECT',
            },
        },
        workflowNodeName: 'httpClient_1',
    },
] as unknown as WorkflowNodeOutput[];

const context: WorkflowDefinitionCompletionContextI = {
    components: catalog,
    format: 'json',
    functions,
    getNodeOutputs: vi.fn(async () => nodeOutputs),
    kind: 'workflowDefinition',
};

const request = (source: string, marker: string) => ({
    context,
    cursorOffset: source.indexOf(marker) + marker.length,
    source,
});

describe('getWorkflowDefinitionCompletions', () => {
    it('offers earlier nodes after ${', async () => {
        const source =
            '{"triggers": [{"name": "trigger_1"}], "tasks": [{"name": "httpClient_1"}, {"name": "t2", "parameters": {"x": "${"}}]}';

        expect((await getWorkflowDefinitionCompletions(request(source, '"x": "${'))).map((item) => item.label)).toEqual(
            ['trigger_1', 'httpClient_1']
        );
    });

    it('offers output properties by dot path', async () => {
        const source =
            '{"tasks": [{"name": "httpClient_1"}, {"name": "t2", "parameters": {"x": "${httpClient_1.body."}}]}';

        expect((await getWorkflowDefinitionCompletions(request(source, 'body.'))).map((item) => item.label)).toEqual([
            'amount',
        ]);
    });

    it('offers functions in a formula value', async () => {
        const source = '{"tasks": [{"name": "t", "parameters": {"x": "=jo"}}]}';
        const items = await getWorkflowDefinitionCompletions(request(source, '=jo'));

        expect(items[0]).toMatchObject({
            filterText: 'join',
            insertAsSnippet: true,
            insertText: 'join($1)',
            label: 'join(separator, list)',
        });
    });

    it('offers components then actions for a task type', async () => {
        const first = '{"tasks": [{"name": "t", "type": ""}]}';

        expect(
            (await getWorkflowDefinitionCompletions(request(first, '"type": "'))).map((item) => item.insertText)
        ).toEqual(['googleMaps/v1/', 'logger/v1/', 'aiAgent/v1/']);

        const second = '{"tasks": [{"name": "t", "type": "googleMaps/v1/"}]}';

        expect(
            (await getWorkflowDefinitionCompletions(request(second, 'googleMaps/v1/'))).map((item) => item.filterText)
        ).toEqual(['getAddress']);
    });

    it('delegates an inline script to the script completions with the task connections', async () => {
        const source =
            '{"tasks": [{"name": "s", "type": "script/v1/javascript", "connections": [{"componentName": "googleMaps", "name": "gm"}], "parameters": {"input": {"order": 1}, "script": "function perform(input, connection, context) {\\n input."}}]}';

        expect((await getWorkflowDefinitionCompletions(request(source, 'input.'))).map((item) => item.label)).toEqual([
            'order',
        ]);

        const connectionSource = source.replace('input."', 'connection."');

        expect(
            (await getWorkflowDefinitionCompletions(request(connectionSource, 'connection.'))).map((item) => item.label)
        ).toEqual(['gm']);
    });

    it('does not offer types for a type key that is not a task or trigger type', async () => {
        const source = '{"tasks": [{"type": "logger/v1/info", "parameters": {"type": ""}}]}';

        expect(await getWorkflowDefinitionCompletions(request(source, '"parameters": {"type": "'))).toEqual([]);
    });

    it('offers component types for a nested task type', async () => {
        const source = '{"tasks": [{"parameters": {"iteratee": [{"type": ""}]}}]}';

        expect(
            (await getWorkflowDefinitionCompletions(request(source, '"iteratee": [{"type": "'))).map(
                (item) => item.insertText
            )
        ).toEqual(['googleMaps/v1/', 'logger/v1/', 'aiAgent/v1/']);
    });

    it('returns nothing for a script type other than javascript and python', async () => {
        const source =
            '{"tasks": [{"name": "s", "type": "script/v1/java", "parameters": {"input": {"order": 1}, "script": "input."}}]}';

        expect(await getWorkflowDefinitionCompletions(request(source, 'input.'))).toEqual([]);
    });

    it.each([
        ["connection['gm'].", "connection['gm']."],
        ['connection[\\"gm\\"].', 'connection[\\"gm\\"].'],
    ])('offers connection properties for %s in an inline script', async (_label, access) => {
        const source = `{"tasks": [{"name": "s", "type": "script/v1/javascript", "connections": [{"componentName": "googleMaps", "name": "gm"}], "parameters": {"script": "function perform(input, connection, context) {\\n ${access}"}}]}`;

        expect((await getWorkflowDefinitionCompletions(request(source, access))).map((item) => item.label)).toEqual([
            'apiKey',
            'region',
        ]);
    });

    it('returns nothing outside strings', async () => {
        expect(await getWorkflowDefinitionCompletions({context, cursorOffset: 1, source: '{ "tasks": [] }'})).toEqual(
            []
        );
    });
});

describe('parameter key completions', () => {
    const jsonContext = {...context, format: 'json' as const};
    const yamlContext = {...context, format: 'yaml' as const};

    it('offers the action properties not yet present in a JSON parameters key', async () => {
        const source = '{"tasks": [{"name": "h", "type": "httpClient/v1/get", "parameters": {"uri": "x", "": 1}}]}';
        const items = await getWorkflowDefinitionCompletions({
            context: jsonContext,
            cursorOffset: source.indexOf('"": 1') + 1,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['headers']);
        expect(catalog.getOperationProperties).toHaveBeenCalledWith('httpClient', 1, 'get', 'action');
    });

    it('offers nested object properties', async () => {
        const source = '{"tasks": [{"type": "httpClient/v1/get", "parameters": {"headers": {""}}}]}';
        const items = await getWorkflowDefinitionCompletions({
            context: jsonContext,
            cursorOffset: source.indexOf('{""}') + 2,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['accept']);
    });

    it('offers trigger properties in a YAML trigger', async () => {
        const source = 'triggers:\n  - name: t\n    type: slack/v1/newMessage\n    parameters:\n      ch';
        const items = await getWorkflowDefinitionCompletions({
            context: yamlContext,
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['channel']);
        expect(catalog.getOperationProperties).toHaveBeenCalledWith('slack', 1, 'newMessage', 'trigger');
    });

    it('treats a task nested in a dispatcher as an action', async () => {
        const source =
            'tasks:\n  - type: loop/v1\n    parameters:\n      iteratee:\n        - type: httpClient/v1/get\n          parameters:\n            ';
        const items = await getWorkflowDefinitionCompletions({
            context: yamlContext,
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['uri', 'headers']);
    });

    it('offers nothing when the node has no type yet', async () => {
        const source = '{"tasks": [{"name": "h", "parameters": {""}}]}';

        expect(
            await getWorkflowDefinitionCompletions({
                context: jsonContext,
                cursorOffset: source.indexOf('{""}') + 2,
                source,
            })
        ).toEqual([]);
    });

    it('offers nothing for a key outside parameters', async () => {
        const source = '{"tasks": [{"type": "httpClient/v1/get", "": 1}]}';

        expect(
            await getWorkflowDefinitionCompletions({
                context: jsonContext,
                cursorOffset: source.indexOf('"": 1') + 1,
                source,
            })
        ).toEqual([]);
    });
});

describe('parameter key completions with runtime properties', () => {
    const properties = [
        {name: 'baseId', type: 'STRING'},
        {name: 'format', type: 'STRING'},
        {displayCondition: "format == 'xml'", name: 'xmlRoot', type: 'STRING'},
        {name: 'fields', propertiesDataSource: {propertiesLookupDependsOn: ['baseId']}, type: 'DYNAMIC_PROPERTIES'},
    ] as unknown as Property[];

    const runtimeCatalog: ComponentCatalogType = {
        ...catalog,
        getDisplayConditions: vi.fn(async () => ({})),
        getOperationProperties: vi.fn(async () => properties),
    };

    it('loads dynamic properties with the task name and the values written in the document', async () => {
        const loadDynamicProperties = vi.fn(async () => [{name: 'Email', type: 'STRING'}] as unknown as Property[]);
        const source =
            '{"tasks": [{"name": "airtable_1", "type": "airtable/v1/createRecord", "parameters": {"baseId": "app1", "fields": {""}}}]}';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: runtimeCatalog, format: 'json', loadDynamicProperties},
            cursorOffset: source.indexOf('{""}') + 2,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['Email']);
        expect(loadDynamicProperties).toHaveBeenCalledWith({
            componentName: 'airtable',
            componentVersion: 1,
            lookupDependsOnPaths: ['baseId'],
            operationKind: 'action',
            operationName: 'createRecord',
            parameters: {baseId: 'app1'},
            propertyName: 'fields',
            workflowNodeName: 'airtable_1',
        });
    });

    it('loads trigger dynamic properties for a trigger', async () => {
        const loadDynamicProperties = vi.fn(async () => [{name: 'Email', type: 'STRING'}] as unknown as Property[]);
        const source =
            'triggers:\n  - name: newRecord_1\n    type: airtable/v1/newRecord\n    parameters:\n      baseId: app1\n      fields:\n        ';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: runtimeCatalog, format: 'yaml', loadDynamicProperties},
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['Email']);
        expect(loadDynamicProperties).toHaveBeenCalledWith(
            expect.objectContaining({
                operationKind: 'trigger',
                operationName: 'newRecord',
                parameters: {baseId: 'app1'},
                workflowNodeName: 'newRecord_1',
            })
        );
    });

    it('hides a property whose display condition is false for the values in the document', async () => {
        const source =
            'tasks:\n  - name: airtable_1\n    type: airtable/v1/createRecord\n    parameters:\n      format: json\n      ';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: runtimeCatalog, format: 'yaml'},
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.label)).toEqual(['baseId', 'fields']);
        expect(runtimeCatalog.getDisplayConditions).toHaveBeenCalledWith('airtable', 1, 'createRecord', 'action', {
            format: 'json',
        });
    });
});

describe('parameter value completions', () => {
    const properties = [
        {name: 'baseId', type: 'STRING'},
        {name: 'note', options: [{label: 'Quoted', value: 'say "hi"'}, {value: 'yes'}], type: 'STRING'},
        {name: 'count', options: [{value: 1}, {value: 2}], type: 'INTEGER'},
        {name: 'tableId', optionsDataSource: {optionsLookupDependsOn: ['baseId']}, type: 'STRING'},
    ] as unknown as Property[];

    const valueCatalog: ComponentCatalogType = {
        ...catalog,
        getOperationProperties: vi.fn(async () => properties),
    };

    it('escapes values inside a JSON string and replaces what was typed', async () => {
        const source = '{"tasks": [{"name": "a_1", "type": "airtable/v1/createRecord", "parameters": {"note": "sa"}}]}';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: valueCatalog, format: 'json'},
            cursorOffset: source.indexOf('"sa"') + 3,
            source,
        });

        expect(items.map((item) => [item.label, item.insertText, item.replaceLength])).toEqual([
            ['Quoted', 'say \\"hi\\"', 2],
            ['yes', 'yes', 2],
        ]);
    });

    it('offers no number options inside a JSON string', async () => {
        const source = '{"tasks": [{"type": "airtable/v1/createRecord", "parameters": {"count": ""}}]}';

        expect(
            await getWorkflowDefinitionCompletions({
                context: {...context, components: valueCatalog, format: 'json'},
                cursorOffset: source.indexOf('""') + 1,
                source,
            })
        ).toEqual([]);
    });

    it('writes plain YAML values, quoting strings YAML would read as something else', async () => {
        const source = 'tasks:\n  - type: airtable/v1/createRecord\n    parameters:\n      note: y';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: valueCatalog, format: 'yaml'},
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => item.insertText)).toEqual(['"say \\"hi\\""', '"yes"']);

        const countSource = 'tasks:\n  - type: airtable/v1/createRecord\n    parameters:\n      count: ';
        const countItems = await getWorkflowDefinitionCompletions({
            context: {...context, components: valueCatalog, format: 'yaml'},
            cursorOffset: countSource.length,
            source: countSource,
        });

        expect(countItems.map((item) => item.insertText)).toEqual(['1', '2']);
    });

    it('loads options from the connection for a saved node', async () => {
        const loadOptions = vi.fn(async () => [{label: 'Customers', value: 'tbl1'}]);
        const source =
            'tasks:\n  - name: a_1\n    type: airtable/v1/createRecord\n    parameters:\n      baseId: app1\n      tableId: ';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: valueCatalog, format: 'yaml', loadOptions},
            cursorOffset: source.length,
            source,
        });

        expect(items.map((item) => [item.label, item.insertText])).toEqual([['Customers', 'tbl1']]);
        expect(loadOptions).toHaveBeenCalledWith({
            componentName: 'airtable',
            componentVersion: 1,
            lookupDependsOnPaths: ['baseId'],
            operationKind: 'action',
            operationName: 'createRecord',
            parameters: {baseId: 'app1'},
            propertyName: 'tableId',
            workflowNodeName: 'a_1',
        });
    });

    it('keeps offering expressions once a value starts one', async () => {
        const source =
            '{"triggers": [{"name": "trigger_1"}], "tasks": [{"type": "airtable/v1/createRecord", "parameters": {"note": "${"}}]}';
        const items = await getWorkflowDefinitionCompletions({
            context: {...context, components: valueCatalog, format: 'json'},
            cursorOffset: source.indexOf('${') + 2,
            source,
        });

        expect(items.map((item) => item.label)).toContain('trigger_1');
    });
});
