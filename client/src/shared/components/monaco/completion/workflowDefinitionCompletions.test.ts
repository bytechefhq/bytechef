import {describe, expect, it, vi} from 'vitest';

import {getWorkflowDefinitionCompletions} from './workflowDefinitionCompletions';

import type {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import type {
    ComponentDefinition,
    ComponentDefinitionBasic,
    ConnectionDefinition,
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
