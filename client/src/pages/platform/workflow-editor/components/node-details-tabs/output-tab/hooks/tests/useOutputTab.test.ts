import {TriggerType} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    clusterMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    onDeleteSuccess: undefined as (() => void) | undefined,
    saveWorkflowNodeTestOutputMutate: vi.fn(),
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
    useSaveWorkflowNodeTestOutputMutation: () => ({mutate: hoisted.saveWorkflowNodeTestOutputMutate}),
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

type SaveWorkflowNodeTestOutputSuccessType = (
    workflowNodeTestOutput: object | null,
    variables: {workflowNodeName: string}
) => void;

function resolveLastWorkflowNodeTestOutput(workflowNodeTestOutput: object | null) {
    const [variables, options] = hoisted.saveWorkflowNodeTestOutputMutate.mock.lastCall as [
        {workflowNodeName: string},
        {onSuccess: SaveWorkflowNodeTestOutputSuccessType},
    ];

    act(() => {
        options.onSuccess(workflowNodeTestOutput, variables);
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

    describe('a test that returns no output', () => {
        const renderDataStorageNode = () =>
            renderHook(({currentNode}) => useOutputTab({currentNode, workflowId: 'wf-1'}), {
                initialProps: {currentNode: {name: 'dataStorage_1'} as NodeDataType},
            });

        it('reports that the test returned no output when the server answers without one', () => {
            const {result} = renderDataStorageNode();

            act(() => {
                result.current.handleTestOperationClick();
            });

            resolveLastWorkflowNodeTestOutput(null);

            expect(result.current.testReturnedNoOutput).toBe(true);
        });

        it('does not report it when the test returned an output', () => {
            const {result} = renderDataStorageNode();

            act(() => {
                result.current.handleTestOperationClick();
            });

            resolveLastWorkflowNodeTestOutput({id: 'test-output-1'});

            expect(result.current.testReturnedNoOutput).toBe(false);
        });

        it('clears the report when the test is run again', () => {
            const {result} = renderDataStorageNode();

            act(() => {
                result.current.handleTestOperationClick();
            });

            resolveLastWorkflowNodeTestOutput(null);

            act(() => {
                result.current.handleTestOperationClick();
            });

            expect(result.current.testReturnedNoOutput).toBe(false);
        });

        it('clears the report when it is dismissed', () => {
            const {result} = renderDataStorageNode();

            act(() => {
                result.current.handleTestOperationClick();
            });

            resolveLastWorkflowNodeTestOutput(null);

            act(() => {
                result.current.handleNoOutputNoticeDismiss();
            });

            expect(result.current.testReturnedNoOutput).toBe(false);
        });

        it('does not carry the report over to another node', () => {
            const {rerender, result} = renderDataStorageNode();

            act(() => {
                result.current.handleTestOperationClick();
            });

            resolveLastWorkflowNodeTestOutput(null);

            rerender({currentNode: {name: 'dataStorage_2'} as NodeDataType});

            expect(result.current.testReturnedNoOutput).toBe(false);
        });

        it('reports that a tool test returned no output and still closes the properties popover', () => {
            const onSuccess = vi.fn();

            const {result} = renderHook(() =>
                useOutputTab({
                    clusterElementType: 'tools',
                    currentNode: {name: 'dataStorage_1', workflowNodeName: 'dataStorage_1'} as NodeDataType,
                    parentWorkflowNodeName: 'aiAgent_1',
                    workflowId: 'wf-1',
                })
            );

            act(() => {
                result.current.handleClusterElementTestSubmit({key: 'tokic'}, onSuccess);
            });

            const [, options] = hoisted.clusterMutate.mock.lastCall as [
                unknown,
                {onSuccess: (data: {saveClusterElementTestOutput: object | null}) => void},
            ];

            act(() => {
                options.onSuccess({saveClusterElementTestOutput: null});
            });

            expect(result.current.testReturnedNoOutput).toBe(true);
            expect(onSuccess).toHaveBeenCalledTimes(1);
        });

        it('does not report it when a tool test returned an output', () => {
            const {result} = renderHook(() =>
                useOutputTab({
                    clusterElementType: 'tools',
                    currentNode: {name: 'dataStorage_1', workflowNodeName: 'dataStorage_1'} as NodeDataType,
                    parentWorkflowNodeName: 'aiAgent_1',
                    workflowId: 'wf-1',
                })
            );

            act(() => {
                result.current.handleClusterElementTestSubmit({key: 'tokic'});
            });

            const [, options] = hoisted.clusterMutate.mock.lastCall as [
                unknown,
                {onSuccess: (data: {saveClusterElementTestOutput: object | null}) => void},
            ];

            act(() => {
                options.onSuccess({saveClusterElementTestOutput: {id: 1}});
            });

            expect(result.current.testReturnedNoOutput).toBe(false);
        });
    });
});
