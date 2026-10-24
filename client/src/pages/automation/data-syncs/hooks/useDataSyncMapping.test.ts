import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {createElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import useDataSyncMapping from './useDataSyncMapping';

const {getOptionsMock, setElementMock, updateElementMock} = vi.hoisted(() => ({
    getOptionsMock: vi.fn(),
    setElementMock: vi.fn(),
    updateElementMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useSetDataSyncElementMutation: () => ({isPending: false, mutate: setElementMock}),
        useUpdateDataSyncElementMutation: () => ({isPending: false, mutate: updateElementMock}),
    };
});

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeOptionApi: class {
        getClusterElementNodeOptions = getOptionsMock;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const wrapper = ({children}: {children: React.ReactNode}) =>
    createElement(QueryClientProvider, {client: new QueryClient()}, children);

const source = {
    componentName: 'csvFile',
    componentVersion: 1,
    id: '1',
    kind: DataSyncElementKind.Source,
    operationName: 'read',
};
const destination = {
    componentName: 'pg',
    componentVersion: 1,
    id: '2',
    kind: DataSyncElementKind.Destination,
    operationName: 'insert',
};
const processor = {
    componentName: 'dataStreamProcessor',
    componentVersion: 1,
    id: '3',
    kind: DataSyncElementKind.Processor,
    operationName: 'fieldMapper',
    parameters: {mappings: [{destinationField: 'email_address', sourceField: 'email'}]},
};

describe('useDataSyncMapping', () => {
    it('creates the field-mapper row once both sides exist and it is missing', () => {
        renderHook(
            () =>
                useDataSyncMapping({
                    dataSync: {draftWorkflowId: 'wf', elements: [source, destination], id: '10'} as never,
                }),
            {wrapper}
        );

        expect(setElementMock).toHaveBeenCalledTimes(1);
        expect(setElementMock).toHaveBeenCalledWith(
            {
                input: {
                    componentName: 'dataStreamProcessor',
                    componentVersion: 1,
                    connectionId: null,
                    dataSyncId: '10',
                    kind: DataSyncElementKind.Processor,
                    operationName: 'fieldMapper',
                    parameters: {mappings: []},
                },
            },
            {onError: expect.any(Function)}
        );
    });

    it('resets the creation guard on a failed create so the next render can retry', () => {
        setElementMock.mockReset();
        setElementMock.mockImplementation((_input: unknown, options?: {onError?: () => void}) => {
            options?.onError?.();
        });

        const {rerender} = renderHook(
            () =>
                useDataSyncMapping({
                    dataSync: {draftWorkflowId: 'wf', elements: [source, destination], id: '10'} as never,
                }),
            {wrapper}
        );

        expect(setElementMock).toHaveBeenCalledTimes(1);

        rerender();

        expect(setElementMock).toHaveBeenCalledTimes(2);
    });

    it('auto-maps fields present on both sides and saves them', async () => {
        getOptionsMock.mockImplementation(({propertyName}: {propertyName: string}) =>
            Promise.resolve(
                propertyName.endsWith('sourceField')
                    ? [{value: 'email'}, {value: 'name'}, {value: 'phone'}]
                    : [{value: 'email'}, {value: 'name'}, {value: 'city'}]
            )
        );

        const {result} = renderHook(
            () =>
                useDataSyncMapping({
                    dataSync: {draftWorkflowId: 'wf', elements: [source, destination, processor], id: '10'} as never,
                }),
            {wrapper}
        );

        await waitFor(() => expect(result.current.sourceOptions).toHaveLength(3));

        await act(async () => {
            await result.current.handleAutoMap();
        });

        expect(result.current.mappings).toEqual([
            {destinationField: 'email', sourceField: 'email'},
            {destinationField: 'name', sourceField: 'name'},
        ]);

        await waitFor(
            () => {
                expect(updateElementMock).toHaveBeenCalledWith({
                    input: {
                        connectionId: null,
                        id: '3',
                        parameters: {
                            mappings: [
                                {destinationField: 'email', sourceField: 'email'},
                                {destinationField: 'name', sourceField: 'name'},
                            ],
                        },
                    },
                });
            },
            {interval: 20, timeout: 3000}
        );
    });
});
