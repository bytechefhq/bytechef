import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpToolSeeding from '../useMcpToolSeeding';

const hoisted = vi.hoisted(() => ({
    definitionsByTool: {} as Record<string, unknown[]>,
    displayConditions: {} as Record<string, boolean> | null,
    fetcher: vi.fn(),
    optionsByConnection: {} as Record<string, Array<{label: string; value: string}>>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ClusterElementDefinitionDocument: 'clusterElementDefinition',
    ClusterElementOptionsDocument: 'clusterElementOptions',
    ComponentOperationType: {ClusterElement: 'CLUSTER_ELEMENT'},
    ComponentPropertyDisplayConditionsDocument: 'componentPropertyDisplayConditions',
}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (document: string, variables: Record<string, unknown>) => () => {
        hoisted.fetcher(document, variables);

        if (document === 'clusterElementDefinition') {
            return Promise.resolve({
                clusterElementDefinition: {
                    properties: hoisted.definitionsByTool[variables.clusterElementName as string] ?? [],
                },
            });
        }

        if (document === 'componentPropertyDisplayConditions') {
            return hoisted.displayConditions
                ? Promise.resolve({componentPropertyDisplayConditions: hoisted.displayConditions})
                : Promise.reject(new Error('no display conditions'));
        }

        const options = hoisted.optionsByConnection[String(variables.connectionId)];

        return options ? Promise.resolve({clusterElementOptions: options}) : Promise.reject(new Error('no options'));
    },
}));

const messageProperty = {
    controlType: 'TEXT',
    description: 'The message',
    name: 'message',
    required: true,
    type: 'STRING',
};

const channelProperty = {
    controlType: 'SELECT',
    name: 'channel',
    optionsDataSource: {optionsLookupDependsOn: []},
    type: 'STRING',
};

const defaultedProperty = {controlType: 'TEXT', defaultValue: 'hello', name: 'greeting', type: 'STRING'};

interface SeedingPropsI {
    newToolNames: string[];
}

const renderSeeding = (initialProps: SeedingPropsI) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const wrapper = ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);

    return renderHook(
        ({newToolNames}: SeedingPropsI) =>
            useMcpToolSeeding({
                componentName: 'slack',
                componentVersion: 1,
                newToolNames,
            }),
        {initialProps, wrapper}
    );
};

const optionsFetches = () => hoisted.fetcher.mock.calls.filter(([document]) => document === 'clusterElementOptions');

const definitionFetchesFor = (toolName: string) =>
    hoisted.fetcher.mock.calls.filter(
        ([document, variables]) =>
            document === 'clusterElementDefinition' &&
            (variables as {clusterElementName: string}).clusterElementName === toolName
    );

describe('useMcpToolSeeding', () => {
    beforeEach(() => {
        hoisted.definitionsByTool = {};
        hoisted.displayConditions = {};
        hoisted.fetcher.mockReset();
        hoisted.optionsByConnection = {};
    });

    it('seeds new tools with ai values', async () => {
        hoisted.definitionsByTool = {sendMessage: [messageProperty, defaultedProperty]};

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        await waitFor(() => {
            expect(result.current.aiValuesByTool.sendMessage).toBeDefined();
        });

        expect(Object.keys(result.current.aiValuesByTool.sendMessage)).toEqual(['message']);
        expect(result.current.aiValuesByTool.sendMessage.message).toMatch(/^=fromAi\('message'/);
    });

    it('resolves waitForSeeding with the seeded values of every new tool', async () => {
        hoisted.definitionsByTool = {postMessage: [messageProperty], sendMessage: [messageProperty]};

        const {result} = renderSeeding({newToolNames: ['sendMessage', 'postMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(Object.keys(seededValues).sort()).toEqual(['postMessage', 'sendMessage']);
        expect(seededValues.sendMessage.message).toMatch(/^=fromAi\('message'/);
    });

    it('does not fetch definitions for tools that are not new', async () => {
        hoisted.definitionsByTool = {sendMessage: [messageProperty]};

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        await act(async () => {
            await result.current.waitForSeeding();
        });

        expect(definitionFetchesFor('sendMessage')).toHaveLength(1);
        expect(definitionFetchesFor('postMessage')).toHaveLength(0);
    });

    it('leaves a select whose options load from the connection empty and loads no options', async () => {
        hoisted.definitionsByTool = {sendMessage: [channelProperty, messageProperty]};

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(optionsFetches()).toHaveLength(0);
        expect(Object.keys(seededValues.sendMessage)).toEqual(['message']);
    });

    it('seeds a select with fixed options together with its options', async () => {
        hoisted.definitionsByTool = {
            sendMessage: [
                {
                    controlType: 'SELECT',
                    name: 'priority',
                    options: [{value: 'high'}, {value: 'low'}],
                    type: 'STRING',
                },
            ],
        };

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(seededValues.sendMessage.priority).toMatch(/^=fromAi\('priority'.*high.*low/);
    });

    it('seeds conditional fields visible under the built-in defaults and skips hidden ones', async () => {
        hoisted.definitionsByTool = {
            sendMessage: [
                {controlType: 'SELECT', defaultValue: 'a', name: 'mode', options: [{value: 'a'}], type: 'STRING'},
                {controlType: 'TEXT', displayCondition: "mode == 'a'", name: 'shownField', type: 'STRING'},
                {controlType: 'TEXT', displayCondition: "mode == 'b'", name: 'hiddenField', type: 'STRING'},
            ],
        };
        hoisted.displayConditions = {"mode == 'a'": true, "mode == 'b'": false};

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(Object.keys(seededValues.sendMessage)).toEqual(['shownField']);
        expect(hoisted.fetcher).toHaveBeenCalledWith('componentPropertyDisplayConditions', {
            componentName: 'slack',
            componentVersion: 1,
            operationName: 'sendMessage',
            operationType: 'CLUSTER_ELEMENT',
            parameters: {mode: 'a'},
        });
    });

    it('skips conditional fields when their display conditions cannot be evaluated', async () => {
        hoisted.definitionsByTool = {
            sendMessage: [
                messageProperty,
                {controlType: 'TEXT', displayCondition: "mode == 'a'", name: 'shownField', type: 'STRING'},
            ],
        };
        hoisted.displayConditions = null;

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(Object.keys(seededValues.sendMessage)).toEqual(['message']);
    });

    it('evaluates no display conditions for a tool without conditional fields', async () => {
        hoisted.definitionsByTool = {sendMessage: [messageProperty]};

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        await act(async () => {
            await result.current.waitForSeeding();
        });

        expect(
            hoisted.fetcher.mock.calls.filter(([document]) => document === 'componentPropertyDisplayConditions')
        ).toHaveLength(0);
    });

    it('does not seed an object sub-property that has a built-in default', async () => {
        hoisted.definitionsByTool = {
            sendMessage: [
                {
                    name: 'body',
                    properties: [
                        {controlType: 'TEXT', defaultValue: 'JSON', name: 'format', type: 'STRING'},
                        {controlType: 'TEXT', name: 'content', type: 'STRING'},
                    ],
                    type: 'OBJECT',
                },
            ],
        };

        const {result} = renderSeeding({newToolNames: ['sendMessage']});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(Object.keys(seededValues.sendMessage)).toEqual(['body.content']);
    });
});
