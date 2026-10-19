import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useDynamicPropertiesLoader} from './useDynamicPropertiesLoader';

const {getWorkflowNodeComponentActionDynamicPropertiesMock} = vi.hoisted(() => ({
    getWorkflowNodeComponentActionDynamicPropertiesMock: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeDynamicPropertiesApi: class {
        getWorkflowNodeComponentActionDynamicProperties = getWorkflowNodeComponentActionDynamicPropertiesMock;
    },
}));

const request = {
    actionName: 'createRecord',
    componentName: 'airtable',
    componentVersion: 1,
    connectionKey: 'at',
    lookupDependsOnPaths: ['baseId'],
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

        getWorkflowNodeComponentActionDynamicPropertiesMock.mockResolvedValue([{name: 'Email'}]);
    });

    it('has no loader without a workflow', () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader(undefined, 1), {wrapper});

        expect(result.current).toBeUndefined();
    });

    it('requests the dynamic properties of the node once for the same values', async () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader('workflow1', 2), {wrapper});

        expect(await result.current!(request)).toEqual([{name: 'Email'}]);

        await result.current!(request);

        expect(getWorkflowNodeComponentActionDynamicPropertiesMock).toHaveBeenCalledTimes(1);
        expect(getWorkflowNodeComponentActionDynamicPropertiesMock).toHaveBeenCalledWith({
            componentActionDynamicPropertiesRequest: {
                actionName: 'createRecord',
                componentName: 'airtable',
                componentVersion: 1,
                connectionKey: 'at',
                lookupDependsOnPaths: ['baseId'],
                parameters: {baseId: 'app1'},
            },
            environmentId: 2,
            id: 'workflow1',
            propertyName: 'fields',
            workflowNodeName: 'script_1',
        });
    });

    it('requests again when the written values change', async () => {
        const {result} = renderHook(() => useDynamicPropertiesLoader('workflow1', 2), {wrapper});

        await result.current!(request);
        await result.current!({...request, parameters: {baseId: 'app2'}});

        expect(getWorkflowNodeComponentActionDynamicPropertiesMock).toHaveBeenCalledTimes(2);
    });
});
