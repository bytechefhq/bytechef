import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useDynamicPropertiesLoader} from './useDynamicPropertiesLoader';

import type {DynamicPropertiesRequestType} from './completionTypes';

const {getWorkflowNodeComponentOperationDynamicPropertiesMock} = vi.hoisted(() => ({
    getWorkflowNodeComponentOperationDynamicPropertiesMock: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ComponentOperationDynamicPropertiesRequestOperationTypeEnum: {Action: 'ACTION', Trigger: 'TRIGGER'},
    WorkflowNodeDynamicPropertiesApi: class {
        getWorkflowNodeComponentOperationDynamicProperties = getWorkflowNodeComponentOperationDynamicPropertiesMock;
    },
}));

const request: DynamicPropertiesRequestType = {
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

describe('useDynamicPropertiesLoader', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        getWorkflowNodeComponentOperationDynamicPropertiesMock.mockResolvedValue([{name: 'Email'}]);
    });

    it('has no loader without a workflow', () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader(undefined, 1), {wrapper});

        expect(result.current).toBeUndefined();
    });

    it('requests the dynamic properties of the node once for the same values', async () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader('workflow1', 2), {wrapper});

        expect(await result.current!(request)).toEqual([{name: 'Email'}]);

        await result.current!(request);

        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledTimes(1);
        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledWith({
            componentOperationDynamicPropertiesRequest: {
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

    it('sends a trigger operation and the cluster element that declares the connection', async () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader('workflow1', 2), {wrapper});

        await result.current!({
            ...request,
            clusterElementNode: {clusterElementType: 'tools', clusterElementWorkflowNodeName: 'script_1'},
            operationKind: 'trigger',
            workflowNodeName: 'aiAgent_1',
        });

        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledWith(
            expect.objectContaining({
                componentOperationDynamicPropertiesRequest: expect.objectContaining({
                    clusterElementType: 'tools',
                    clusterElementWorkflowNodeName: 'script_1',
                    operationType: 'TRIGGER',
                }),
                workflowNodeName: 'aiAgent_1',
            })
        );
    });

    it('requests again when the written values change', async () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader('workflow1', 2), {wrapper});

        await result.current!(request);
        await result.current!({...request, parameters: {baseId: 'app2'}});

        expect(getWorkflowNodeComponentOperationDynamicPropertiesMock).toHaveBeenCalledTimes(2);
    });
});
