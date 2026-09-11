import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    onDeleteSuccess: undefined as (() => void) | undefined,
}));

vi.mock('@tanstack/react-query', async () => ({
    ...(await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query')),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/mutations/platform/workflowNodeTestOutputs.mutations', () => ({
    useDeleteWorkflowNodeTestOutputMutation: ({onSuccess}: {onSuccess?: () => void}) => {
        hoisted.onDeleteSuccess = onSuccess;

        return {mutate: () => onSuccess?.()};
    },
    useSaveWorkflowNodeTestOutputMutation: () => ({mutate: vi.fn()}),
    useUploadSampleOutputRequestMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveClusterElementTestOutputMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    WorkflowNodeOutputKeys: {
        workflowNodeOutput: () => ['workflowNodeOutput'],
        workflowNodeOutputs: ['workflowNodeOutputs'],
    },
    useGetClusterElementOutputQuery: () => ({data: undefined, isFetching: false, refetch: vi.fn()}),
    useGetWorkflowNodeOutputQuery: () => ({data: undefined, isFetching: false, refetch: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/workflowNodeTestOutputs.queries', () => ({
    useCheckWorkflowNodeTestOutputExistsQuery: () => ({refetch: vi.fn()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({webhookTriggerTestApi: undefined}),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, vi.fn()],
}));

import useOutputTab from '../useOutputTab';

describe('useOutputTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('re-runs the workflow validation after the node test output is reset', () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'httpClient_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handlePredefinedOutputSchemaClick();
        });

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['workflowNodeOutputs', 'wf-1']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['ValidateWorkflow']});
    });
});
