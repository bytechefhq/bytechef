import {beforeEach, describe, expect, test, vi} from 'vitest';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from './deleteProperty';

vi.mock('./workflowMutationQueue', () => ({
    enqueueWorkflowMutation: (execute: () => Promise<unknown>) => execute(),
}));

function createMockMutation(
    mutateAsync = vi.fn().mockResolvedValue({displayConditions: {}, metadata: {}, parameters: {}})
) {
    return {
        context: undefined,
        data: undefined,
        error: null,
        failureCount: 0,
        failureReason: null,
        isError: false as const,
        isIdle: true as const,
        isPaused: false,
        isPending: false as const,
        isSuccess: false as const,
        mutate: vi.fn(),
        mutateAsync,
        reset: vi.fn(),
        status: 'idle' as const,
        submittedAt: 0,
        variables: undefined,
    };
}

describe('deleteProperty', () => {
    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: undefined,
            currentNode: undefined,
        });

        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: undefined,
        });
    });

    test('uses currentNode.workflowNodeName when rootClusterElementNodeData is undefined for a regular node', async () => {
        const mutateAsync = vi.fn().mockResolvedValue({displayConditions: {}, metadata: {}, parameters: {}});

        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: {componentName: 'test', workflowNodeName: 'test_1'} as never,
            currentNode: {componentName: 'test', workflowNodeName: 'test_1'} as never,
        });

        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: undefined,
        });

        deleteProperty('workflow-1', 'parameters.field', createMockMutation(mutateAsync));

        await vi.waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledTimes(1);
        });

        const callArgs = mutateAsync.mock.calls[0][0];

        expect(callArgs.workflowNodeName).toBe('test_1');
    });

    test('uses rootClusterElementNodeData.workflowNodeName when it is available', async () => {
        const mutateAsync = vi.fn().mockResolvedValue({displayConditions: {}, metadata: {}, parameters: {}});

        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: {componentName: 'test', workflowNodeName: 'test_1'} as never,
            currentNode: {componentName: 'test', workflowNodeName: 'test_1'} as never,
        });

        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {componentName: 'root', workflowNodeName: 'root_1'} as never,
        });

        deleteProperty('workflow-1', 'parameters.field', createMockMutation(mutateAsync));

        await vi.waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledTimes(1);
        });

        const callArgs = mutateAsync.mock.calls[0][0];

        expect(callArgs.workflowNodeName).toBe('root_1');
    });
});
