import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useOperationPropertyLoaders} from './useOperationPropertyLoaders';

import type {OperationPropertyRequestType} from './completionTypes';

const {getWorkflowNodeComponentOperationDynamicPropertiesMock, getWorkflowNodeComponentOperationOptionsMock} =
    vi.hoisted(() => ({
        getWorkflowNodeComponentOperationDynamicPropertiesMock: vi.fn(),
        getWorkflowNodeComponentOperationOptionsMock: vi.fn(),
    }));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ComponentOperationPropertyRequestOperationTypeEnum: {Action: 'ACTION', Trigger: 'TRIGGER'},
    WorkflowNodeDynamicPropertiesApi: class {
        getWorkflowNodeComponentOperationDynamicProperties = getWorkflowNodeComponentOperationDynamicPropertiesMock;
    },
    WorkflowNodeOptionApi: class {
        getWorkflowNodeComponentOperationOptions = getWorkflowNodeComponentOperationOptionsMock;
    },
}));

const request: OperationPropertyRequestType = {
    componentName: 'airtable',
    componentVersion: 1,
    connectionKey: 'at',
    lookupDependsOnPaths: ['baseId'],
    operationKind: 'action',
    operationName: 'createRecord',
    parameters: {baseId: 'app1'},
    propertyName: 'fields',
    workflowNodeName: 'script_1',
};

let queryClient: QueryClient;

function wrapper({children}: {children: ReactNode}) {
    return createElement(QueryClientProvider, {client: queryClient}, children);
}

describe('useOperationPropertyLoaders', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        getWorkflowNodeComponentOperationDynamicPropertiesMock.mockResolvedValue([{name: 'Email'}]);
        getWorkflowNodeComponentOperationOptionsMock.mockResolvedValue([{label: 'Customers', value: 'tbl1'}]);
    });

    it('has no loaders without a workflow', () => {
        const {result} = renderHook(() => useOperationPropertyLoaders(undefined, 1), {wrapper});

        expect(result.current).toEqual({});
    });

    it('requests the dynamic properties of the node once for the same values', async () => {
        const {result} = renderHook(() => useOperationPropertyLoaders('workflow1', 2), {wrapper});

        expect(await result.current.loadDynamicProperties!(request)).toEqual([{name: 'Email'}]);

        await result.current.loadDynamicProperties!(request);

        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledTimes(1);
        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledWith({
            componentOperationPropertyRequest: {
                componentName: 'airtable',
                componentVersion: 1,
                connectionKey: 'at',
                lookupDependsOnPaths: ['baseId'],
                operationName: 'createRecord',
                operationType: 'ACTION',
                parameters: {baseId: 'app1'},
            },
            environmentId: 2,
            id: 'workflow1',
            propertyName: 'fields',
            workflowNodeName: 'script_1',
        });
    });

    it('requests options with a trigger operation and the cluster element that declares the connection', async () => {
        const {result} = renderHook(() => useOperationPropertyLoaders('workflow1', 2), {wrapper});

        expect(
            await result.current.loadOptions!({
                ...request,
                clusterElementNode: {clusterElementType: 'tools', clusterElementWorkflowNodeName: 'script_1'},
                operationKind: 'trigger',
                propertyName: 'tableId',
                workflowNodeName: 'aiAgent_1',
            })
        ).toEqual([{label: 'Customers', value: 'tbl1'}]);

        expect(getWorkflowNodeComponentOperationOptionsMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentOperationPropertyRequest: expect.objectContaining({
                    clusterElementType: 'tools',
                    clusterElementWorkflowNodeName: 'script_1',
                    operationType: 'TRIGGER',
                }),
                propertyName: 'tableId',
                workflowNodeName: 'aiAgent_1',
            })
        );
    });

    it('requests again when the written values change', async () => {
        const {result} = renderHook(() => useOperationPropertyLoaders('workflow1', 2), {wrapper});

        await result.current.loadOptions!(request);
        await result.current.loadOptions!({...request, parameters: {baseId: 'app2'}});

        expect(getWorkflowNodeComponentOperationOptionsMock).toHaveBeenCalledTimes(2);
    });
});

describe('useOperationPropertyLoaders cache', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        getWorkflowNodeComponentOperationOptionsMock.mockResolvedValue([]);
    });

    it('does not request again when only a value outside the lookup paths changes', async () => {
        const {result} = renderHook(() => useOperationPropertyLoaders('workflow1', 2), {wrapper});

        await result.current.loadOptions!({...request, parameters: {baseId: 'app1', note: 'a'}});
        await result.current.loadOptions!({...request, parameters: {baseId: 'app1', note: 'b'}});

        expect(getWorkflowNodeComponentOperationOptionsMock).toHaveBeenCalledTimes(1);
        expect(getWorkflowNodeComponentOperationOptionsMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentOperationPropertyRequest: expect.objectContaining({parameters: {baseId: 'app1', note: 'a'}}),
            })
        );
    });
});
