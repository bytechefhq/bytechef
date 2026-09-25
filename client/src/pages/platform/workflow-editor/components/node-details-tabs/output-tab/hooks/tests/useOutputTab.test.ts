import {ResponseError, TriggerType} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

type MutateOptionsType = {onError?: (error: unknown) => void; onSuccess?: () => void};

const hoisted = vi.hoisted(() => ({
    clusterMutate: vi.fn(),
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    onDeleteSuccess: undefined as (() => void) | undefined,
    saveMutate: vi.fn(),
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

        return {
            mutate: (variables: unknown, options?: unknown) => {
                hoisted.deleteMutate(variables, options);

                onSuccess?.();
            },
        };
    },
    useSaveWorkflowNodeTestOutputMutation: () => ({mutate: hoisted.saveMutate}),
    useUploadSampleOutputRequestMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveClusterElementTestOutputMutation: () => ({mutate: hoisted.clusterMutate}),
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

function failMutateCall(mutate: ReturnType<typeof vi.fn>, callIndex: number, error: unknown) {
    const options = mutate.mock.calls[callIndex][1] as MutateOptionsType;

    act(() => {
        options.onError?.(error);
    });
}

async function flushErrorResolution() {
    await act(async () => {
        await Promise.resolve();
        await Promise.resolve();
    });
}

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

        act(() => {
            result.current.handleTestOperationClick();
        });

        const response = new Response(JSON.stringify({detail: 'All scraping engines failed', title: 'Error'}), {
            status: 500,
        });

        failMutateCall(hoisted.saveMutate, 0, new ResponseError(response, 'Response returned an error code'));

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

    it('falls back to the problem title, then to the status, when the server gives no detail', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        failMutateCall(
            hoisted.saveMutate,
            0,
            new ResponseError(new Response(JSON.stringify({title: 'Bad Gateway'}), {status: 502}))
        );

        await waitFor(() => {
            expect(result.current.testOutputError?.message).toBe('Bad Gateway');
        });

        act(() => {
            result.current.handleTestOperationClick();
        });

        failMutateCall(hoisted.saveMutate, 1, new ResponseError(new Response('not json', {status: 500})));

        await waitFor(() => {
            expect(result.current.testOutputError?.message).toBe('Request failed with status 500');
        });
    });

    it('shows the message of a plain error, and a generic message for a non-error rejection', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        failMutateCall(hoisted.saveMutate, 0, new Error('Network down'));

        await waitFor(() => {
            expect(result.current.testOutputError?.message).toBe('Network down');
        });

        act(() => {
            result.current.handleTestOperationClick();
        });

        failMutateCall(hoisted.saveMutate, 1, 'boom');

        await waitFor(() => {
            expect(result.current.testOutputError?.message).toBe('Unknown error');
        });
    });

    it('ignores a failure that arrives after the test has been run again', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        act(() => {
            result.current.handleTestOperationClick();
        });

        failMutateCall(hoisted.saveMutate, 0, new Error('Stale failure'));

        await flushErrorResolution();

        expect(result.current.testOutputError).toBeUndefined();

        failMutateCall(hoisted.saveMutate, 1, new Error('Current failure'));

        await waitFor(() => {
            expect(result.current.testOutputError?.message).toBe('Current failure');
        });
    });

    it('ignores a failure that arrives after another node has been selected', async () => {
        const {rerender, result} = renderHook(
            ({nodeName}: {nodeName: string}) =>
                useOutputTab({currentNode: {name: nodeName} as NodeDataType, workflowId: 'wf-1'}),
            {initialProps: {nodeName: 'firecrawl_1'}}
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        rerender({nodeName: 'httpClient_1'});

        failMutateCall(hoisted.saveMutate, 0, new Error('Failure of the previous node'));

        await flushErrorResolution();

        expect(result.current.testOutputError).toBeUndefined();
    });

    it('ignores a failure that arrives after the error has been dismissed', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handleTestOperationClick();
        });

        act(() => {
            result.current.clearTestOutputError();
        });

        failMutateCall(hoisted.saveMutate, 0, new Error('Dismissed failure'));

        await flushErrorResolution();

        expect(result.current.testOutputError).toBeUndefined();
    });

    it('titles a failed reset as a reset failure', async () => {
        const {result} = renderHook(() =>
            useOutputTab({currentNode: {name: 'firecrawl_1'} as NodeDataType, workflowId: 'wf-1'})
        );

        act(() => {
            result.current.handlePredefinedOutputSchemaClick();
        });

        failMutateCall(hoisted.deleteMutate, 0, new Error('Could not delete'));

        await waitFor(() => {
            expect(result.current.testOutputError).toEqual({message: 'Could not delete', title: 'Reset failed'});
        });
    });

    it('shows a failed cluster element test inline', async () => {
        const {result} = renderHook(() =>
            useOutputTab({
                clusterElementType: 'tools',
                currentNode: {name: 'firecrawl_1', workflowNodeName: 'firecrawl_1'} as NodeDataType,
                parentWorkflowNodeName: 'aiAgent_1',
                workflowId: 'wf-1',
            })
        );

        act(() => {
            result.current.handleClusterElementTestSubmit({url: 'https://example.org'});
        });

        failMutateCall(hoisted.clusterMutate, 0, new Error('Tool failed'));

        await waitFor(() => {
            expect(result.current.testOutputError).toEqual({message: 'Tool failed', title: 'Test failed'});
        });
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
