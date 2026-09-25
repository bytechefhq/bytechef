import {ResponseError, TriggerType} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    onDeleteSuccess: undefined as (() => void) | undefined,
    onSaveError: undefined as ((error: unknown) => void) | undefined,
    webhookTriggerTestApi: undefined as
        | {startWebhookTriggerTest: ReturnType<typeof vi.fn>; stopWebhookTriggerTest: ReturnType<typeof vi.fn>}
        | undefined,
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
    useSaveWorkflowNodeTestOutputMutation: ({onError}: {onError?: (error: unknown) => void}) => {
        hoisted.onSaveError = onError;

        return {mutate: vi.fn()};
    },
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
    useWorkflowEditor: () => ({webhookTriggerTestApi: hoisted.webhookTriggerTestApi}),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, vi.fn()],
}));

import useOutputTab from '../useOutputTab';

describe('useOutputTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.webhookTriggerTestApi = undefined;
    });

    afterEach(() => {
        vi.useRealTimers();
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

    it('shows the server error of a failed test inline and clears it when the test is run again', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        const response = new Response(JSON.stringify({detail: 'All scraping engines failed', title: 'Error'}), {
            status: 500,
        });

        act(() => {
            hoisted.onSaveError?.(new ResponseError(response, 'Response returned an error code'));
        });

        await waitFor(() => {
            expect(result.current.testOutputError).toEqual({
                message: 'All scraping engines failed',
                title: 'Test failed',
            });
        });

        act(() => {
            result.current.handleTestOperationClick();
        });

        expect(result.current.testOutputError).toBeUndefined();
    });

    it('starts and stops the webhook test for the trigger being tested, not the first trigger', async () => {
        vi.useFakeTimers({shouldAdvanceTime: true});

        hoisted.webhookTriggerTestApi = {
            startWebhookTriggerTest: vi.fn().mockResolvedValue({webhookUrl: 'https://example.org/webhook'}),
            stopWebhookTriggerTest: vi.fn().mockResolvedValue(undefined),
        };

        const {result} = renderHook(() =>
            useOutputTab({
                currentNode: {name: 'trigger_2', trigger: true, triggerType: TriggerType.StaticWebhook} as NodeDataType,
                workflowId: 'wf-1',
            })
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        expect(hoisted.webhookTriggerTestApi.startWebhookTriggerTest).toHaveBeenCalledWith({
            environmentId: 2,
            triggerName: 'trigger_2',
            workflowId: 'wf-1',
        });

        await waitFor(() => {
            expect(result.current.webhookTestCancelEnabled).toBe(true);
        });

        act(() => {
            result.current.handleTestCancelClick();
        });

        expect(hoisted.webhookTriggerTestApi.stopWebhookTriggerTest).toHaveBeenCalledWith({
            environmentId: 2,
            triggerName: 'trigger_2',
            workflowId: 'wf-1',
        });
    });
});
