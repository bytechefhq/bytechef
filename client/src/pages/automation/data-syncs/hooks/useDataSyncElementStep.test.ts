import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataSyncElementStep from './useDataSyncElementStep';

const {fetcherMock, setElementMock, updateElementMock} = vi.hoisted(() => ({
    fetcherMock: vi.fn(),
    setElementMock: vi.fn(),
    updateElementMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useClusterElementDefinitionQuery: () => ({
            data: {
                clusterElementDefinition: {
                    properties: [{name: 'path', type: 'STRING'}],
                },
            },
        }),
        useSetDataSyncElementMutation: () => ({isPending: false, mutate: setElementMock}),
        useUpdateDataSyncElementMutation: () => ({isPending: false, mutate: updateElementMock}),
    };
});

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: fetcherMock,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {clusterElementsCount: {SOURCE: 1}, name: 'csvFile', title: 'CSV File', version: 1},
            {clusterElementsCount: {DESTINATION: 1}, name: 'postgresql', title: 'PostgreSQL', version: 1},
        ],
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: {clusterElements: [{name: 'read', title: 'Read', type: 'SOURCE'}], connection: undefined},
    }),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => ({displayConditions: undefined, isEvaluating: false}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

const wrapper = ({children}: {children: React.ReactNode}) =>
    createElement(QueryClientProvider, {client: new QueryClient()}, children);

const sourceElement = {
    componentName: 'csvFile',
    componentVersion: 1,
    connectionId: null,
    id: '5',
    kind: DataSyncElementKind.Source,
    operationName: 'read',
    parameters: {delimiter: ';'},
};

describe('useDataSyncElementStep', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('lists only components that offer the step kind', () => {
        const {result} = renderHook(
            () =>
                useDataSyncElementStep({
                    dataSync: {elements: [], id: '10'} as never,
                    kind: DataSyncElementKind.Source,
                }),
            {wrapper}
        );

        expect(result.current.candidateDefinitions.map((definition) => definition.name)).toEqual(['csvFile']);
    });

    it('sets the element with schema defaults fetched for the operation being picked, not the stale render-time query', async () => {
        fetcherMock.mockReturnValue(() =>
            Promise.resolve({
                clusterElementDefinition: {
                    properties: [
                        {defaultValue: ',', name: 'delimiter', type: 'STRING'},
                        {name: 'path', type: 'STRING'},
                    ],
                },
            })
        );

        const {result} = renderHook(
            () =>
                useDataSyncElementStep({
                    dataSync: {elements: [], id: '10'} as never,
                    kind: DataSyncElementKind.Source,
                }),
            {wrapper}
        );

        act(() => result.current.handleComponentChange('csvFile'));
        act(() => result.current.handleOperationChange('read'));

        await waitFor(() => {
            expect(setElementMock).toHaveBeenCalled();
        });

        expect(setElementMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({
                    componentName: 'csvFile',
                    componentVersion: 1,
                    dataSyncId: '10',
                    kind: DataSyncElementKind.Source,
                    operationName: 'read',
                    parameters: {delimiter: ','},
                }),
            }),
            expect.anything()
        );
    });

    it('debounces a field edit into one whole-map update', async () => {
        const {result} = renderHook(
            () =>
                useDataSyncElementStep({
                    dataSync: {elements: [sourceElement], id: '10'} as never,
                    kind: DataSyncElementKind.Source,
                }),
            {wrapper}
        );

        act(() => result.current.form.setValue('parameters.path', '/tmp/a.csv'));
        act(() => result.current.form.setValue('parameters.path', '/tmp/b.csv'));

        await waitFor(
            () => {
                expect(updateElementMock).toHaveBeenCalledTimes(1);
            },
            {interval: 20, timeout: 3000}
        );

        expect(updateElementMock).toHaveBeenCalledWith({
            input: {connectionId: null, id: '5', parameters: {delimiter: ';', path: '/tmp/b.csv'}},
        });

        expect(setElementMock).not.toHaveBeenCalled();
    });

    it('does not call either mutation when only picking a component', () => {
        const {result} = renderHook(
            () =>
                useDataSyncElementStep({
                    dataSync: {elements: [], id: '10'} as never,
                    kind: DataSyncElementKind.Source,
                }),
            {wrapper}
        );

        act(() => result.current.handleComponentChange('csvFile'));

        expect(setElementMock).not.toHaveBeenCalled();
        expect(updateElementMock).not.toHaveBeenCalled();
    });

    it('clears the pickers and form when the same hook instance is reused for a kind with no element', () => {
        const dataSync = {elements: [sourceElement], id: '10'} as never;

        const {rerender, result} = renderHook(
            ({kind}: {kind: DataSyncElementKind}) => useDataSyncElementStep({dataSync, kind}),
            {initialProps: {kind: DataSyncElementKind.Source}, wrapper}
        );

        expect(result.current.selectedComponentName).toBe('csvFile');
        expect(result.current.selectedOperationName).toBe('read');
        expect(result.current.form.getValues('parameters')).toEqual({delimiter: ';'});

        rerender({kind: DataSyncElementKind.Destination});

        expect(result.current.selectedComponentName).toBe('');
        expect(result.current.selectedOperationName).toBe('');
        expect(result.current.form.getValues('connectionId')).toBeNull();
        expect(result.current.form.getValues('parameters')).toEqual({});
    });
});
