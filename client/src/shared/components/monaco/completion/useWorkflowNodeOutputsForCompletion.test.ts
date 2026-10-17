import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowNodeOutputsForCompletion} from './useWorkflowNodeOutputsForCompletion';

const {getPreviousWorkflowNodeOutputsMock} = vi.hoisted(() => ({
    getPreviousWorkflowNodeOutputsMock: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeOutputApi: class {
        getPreviousWorkflowNodeOutputs = getPreviousWorkflowNodeOutputsMock;
    },
}));

let queryClient: QueryClient;

function wrapper({children}: {children: ReactNode}) {
    return createElement(QueryClientProvider, {client: queryClient}, children);
}

describe('useWorkflowNodeOutputsForCompletion', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        getPreviousWorkflowNodeOutputsMock.mockResolvedValue([{workflowNodeName: 'trigger_1'}]);
    });

    it('fetches whole-workflow node outputs lazily and once', async () => {
        const {result} = renderHook(() => useWorkflowNodeOutputsForCompletion('wf-1', 7), {wrapper});

        expect(getPreviousWorkflowNodeOutputsMock).not.toHaveBeenCalled();

        await result.current();
        await result.current();

        expect(getPreviousWorkflowNodeOutputsMock).toHaveBeenCalledTimes(1);
        expect(getPreviousWorkflowNodeOutputsMock).toHaveBeenCalledWith({environmentId: 7, id: 'wf-1'});
    });

    it('resolves an empty list without a workflow id', async () => {
        const {result} = renderHook(() => useWorkflowNodeOutputsForCompletion(undefined, 7), {wrapper});

        expect(await result.current()).toEqual([]);
        expect(getPreviousWorkflowNodeOutputsMock).not.toHaveBeenCalled();
    });

    it('keeps the callback stable for the same workflow and environment', () => {
        const {rerender, result} = renderHook(() => useWorkflowNodeOutputsForCompletion('wf-1', 7), {wrapper});

        const first = result.current;

        rerender();

        expect(result.current).toBe(first);
    });
});
