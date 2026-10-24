import {WorkflowTestNodeStateI} from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useSSE} from '@/shared/hooks/useSSE';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowTestStream} from '../useWorkflowTestStream';

const mockRemoveWorkflowTestNodeState = vi.fn();
const mockResetWorkflowTestNodeStates = vi.fn();
const mockSetWorkflowIsRunning = vi.fn();
const mockSetWorkflowTestExecution = vi.fn();
const mockSetWorkflowTestNodeState = vi.fn();

let mockWorkflowIsRunning = false;
let mockWorkflowTestExecution: {job?: {status?: string}} | undefined;
let mockWorkflowTestNodeStates: Record<string, WorkflowTestNodeStateI> = {};

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => {
    // Lazy: the mock* consts are hoisted below this factory, so they must only be dereferenced at call time.
    const getStoreState = () => ({
        removeWorkflowTestNodeState: mockRemoveWorkflowTestNodeState,
        resetWorkflowTestNodeStates: mockResetWorkflowTestNodeStates,
        setWorkflowIsRunning: mockSetWorkflowIsRunning,
        setWorkflowTestExecution: mockSetWorkflowTestExecution,
        setWorkflowTestNodeState: mockSetWorkflowTestNodeState,
        workflowIsRunning: mockWorkflowIsRunning,
        workflowTestExecution: mockWorkflowTestExecution,
        workflowTestNodeStates: mockWorkflowTestNodeStates,
    });

    const useStoreMock = Object.assign(
        vi.fn((selector) => selector(getStoreState())),
        {getState: getStoreState}
    );

    return {
        default: useStoreMock,
        useWorkflowEditorStore: useStoreMock,
    };
});

const mockPersistJobId = vi.fn();
const usePersistJobId = vi.fn();
vi.mock('@/shared/hooks/usePersistJobId', () => ({
    usePersistJobId: vi.fn(() => ({
        persistJobId: mockPersistJobId,
        usePersistJobId: usePersistJobId,
    })),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) =>
        selector({
            currentEnvironmentId: 'env-123',
        })
    ),
}));

const mockClose = vi.fn();
const mockError = null;
vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({
        close: mockClose,
        error: mockError,
    })),
}));

describe('useWorkflowTestStream', () => {
    afterEach(() => {
        mockWorkflowIsRunning = false;
        mockWorkflowTestExecution = undefined;
        mockWorkflowTestNodeStates = {};

        vi.clearAllMocks();
    });

    it('should initialize with null streamRequest', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        expect(useSSE).toHaveBeenCalledWith(null, expect.any(Object));
    });

    it('should call setStreamRequest and trigger useSSE', () => {
        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const mockRequest = {init: {method: 'POST'}, url: '/test'};

        act(() => {
            result.current.setStreamRequest(mockRequest);
        });

        expect(useSSE).toHaveBeenLastCalledWith(mockRequest, expect.any(Object));
    });

    it('should handle start event', () => {
        const onStart = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onStart,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.start({jobId: 'job-123'});
        });

        expect(mockPersistJobId).toHaveBeenCalledWith('job-123');
        expect(onStart).toHaveBeenCalledWith('job-123');
    });

    it('should handle result event', () => {
        const onResult = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {status: 'COMPLETED'}});
        });

        expect(mockSetWorkflowTestExecution).toHaveBeenCalled();
        expect(onResult).toHaveBeenCalled();
        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
    });

    it('should handle error event', () => {
        const onError = vi.fn();
        const errorMessage = 'SSE Error';

        (useSSE as any).mockReturnValueOnce({
            close: mockClose,
            error: errorMessage,
        });

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const {result} = renderHook(() =>
            useWorkflowTestStream({
                onError,
                workflowId: 'workflow-123',
            })
        );

        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.error();
        });

        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
        expect(mockSetWorkflowTestExecution).toHaveBeenCalledWith(undefined);
        expect(onError).toHaveBeenCalled();
        expect(result.current.error).toBe(errorMessage);
    });

    it('should report a malformed ask_user_question event as an error', () => {
        const onError = vi.fn();

        renderHook(() =>
            useWorkflowTestStream({
                onError,
                workflowId: 'workflow-123',
            })
        );

        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.ask_user_question({questions: 'not a list'});
        });

        expect(onError).toHaveBeenCalledWith('The agent asked a question in an unexpected format.');
        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
    });

    it('should handle stream event with valid chunk', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.stream({text: 'streaming text'});
        });
    });

    it('should close the SSE connection and clear nodes left running on close', () => {
        mockWorkflowTestNodeStates = {firecrawl_6: {status: 'COMPLETED'}, openAi_1: {status: 'RUNNING'}};

        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        act(() => {
            result.current.close();
        });

        expect(mockClose).toHaveBeenCalled();
        expect(mockRemoveWorkflowTestNodeState).toHaveBeenCalledWith('openAi_1');
        expect(mockRemoveWorkflowTestNodeState).not.toHaveBeenCalledWith('firecrawl_6');
    });

    it('should clear nodes left running on error event', () => {
        mockWorkflowTestNodeStates = {firecrawl_6: {status: 'COMPLETED'}, firecrawl_7: {status: 'RUNNING'}};

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.error('Aborted');
        });

        expect(mockRemoveWorkflowTestNodeState).toHaveBeenCalledWith('firecrawl_7');
        expect(mockRemoveWorkflowTestNodeState).not.toHaveBeenCalledWith('firecrawl_6');
    });

    it('should return error from useSSE', () => {
        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        expect(result.current.error).toBe(mockError);
    });

    it('should provide persistJobId function', () => {
        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        act(() => {
            result.current.persistJobId('new-job-id');
        });

        expect(mockPersistJobId).toHaveBeenCalledWith('new-job-id');
    });

    it('should handle result with message content', () => {
        const onResult = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {outputs: {message: 'Result message'}, status: 'COMPLETED'}});
        });

        expect(mockPersistJobId).toHaveBeenCalledWith(null);
    });

    it('should handle result with empty message', () => {
        const onResult = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {outputs: {message: ''}, status: 'COMPLETED'}});
        });

        expect(onResult).toHaveBeenCalled();
    });

    it('should handle result with no outputs', () => {
        const onResult = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {status: 'COMPLETED'}});
        });

        expect(onResult).toHaveBeenCalled();
    });

    it('should handle result with string data', () => {
        const onResult = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result('{"job":{"status":"COMPLETED"}}');
        });

        expect(onResult).toHaveBeenCalled();
    });

    it('should handle result with invalid JSON', () => {
        const onResult = vi.fn();
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        renderHook(() =>
            useWorkflowTestStream({
                onResult,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result('{invalid json}');
        });

        expect(consoleErrorSpy).toHaveBeenCalled();
        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
        expect(mockPersistJobId).toHaveBeenCalledWith(null);

        consoleErrorSpy.mockRestore();
    });

    it('should end the run with an error when the result cannot be parsed', () => {
        const onError = vi.fn();
        const onResult = vi.fn();
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        renderHook(() =>
            useWorkflowTestStream({
                onError,
                onResult,
                workflowId: 'workflow-123',
            })
        );

        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result('{invalid json}');
        });

        expect(onResult).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledWith('Failed to read the workflow test result.');
        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);

        consoleErrorSpy.mockRestore();
    });

    it('should handle start event with string data', () => {
        const onStart = vi.fn();
        renderHook(() =>
            useWorkflowTestStream({
                onStart,
                workflowId: 'workflow-123',
            })
        );

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.start('{"jobId":456}');
        });

        expect(onStart).toHaveBeenCalledWith('456');
    });

    it('should handle stream event with empty chunk', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.stream({text: ''});
        });
    });

    it('should reset node states on start event', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.start({jobId: 'job-123'});
        });

        expect(mockResetWorkflowTestNodeStates).toHaveBeenCalled();
    });

    it('should handle task_started event', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_started({name: 'task_1', taskExecutionId: '10'});
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'RUNNING'});
    });

    it('should handle task_completed event', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_completed({name: 'task_1', status: 'COMPLETED', taskExecutionId: '10'});
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'COMPLETED'});
    });

    it('should compute duration for task_completed event with dates', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_completed({
                endDate: '2026-07-20T10:00:01.250Z',
                name: 'task_1',
                startDate: '2026-07-20T10:00:00.000Z',
                status: 'COMPLETED',
                taskExecutionId: '10',
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {
            durationMillis: 1250,
            status: 'COMPLETED',
        });
    });

    it('should handle task_completed event with failed status', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_completed({name: 'task_1', status: 'FAILED', taskExecutionId: '10'});
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'FAILED'});
    });

    it('should handle task_failed event', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_failed({error: 'Boom', name: 'task_1', taskExecutionId: '10'});
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {error: 'Boom', status: 'FAILED'});
    });

    it('should compute duration for task_failed event with dates', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_failed({
                endDate: '2026-07-20T10:00:02.500Z',
                error: 'Boom',
                name: 'task_1',
                startDate: '2026-07-20T10:00:00.000Z',
                taskExecutionId: '10',
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {
            durationMillis: 2500,
            error: 'Boom',
            status: 'FAILED',
        });
    });

    it('should handle task_started event with string data', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_started('{"name":"task_1","taskExecutionId":"10"}');
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'RUNNING'});
    });

    it('should mark a cluster element running and then completed', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.cluster_element_started({
                name: 'httpClient_1',
                startDate: '2026-10-23T10:00:00Z',
                status: 'STARTED',
                taskExecutionId: '10',
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenLastCalledWith('httpClient_1', {
            clusterElement: true,
            status: 'RUNNING',
        });

        act(() => {
            eventHandlers.cluster_element_completed({
                endDate: '2026-10-23T10:00:00.412Z',
                name: 'httpClient_1',
                startDate: '2026-10-23T10:00:00Z',
                status: 'COMPLETED',
                taskExecutionId: '10',
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenLastCalledWith('httpClient_1', {
            clusterElement: true,
            durationMillis: 412,
            status: 'COMPLETED',
        });
    });

    it('should keep a cluster element failed across task executions', () => {
        mockWorkflowTestNodeStates = {httpClient_1: {status: 'FAILED'}};

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.cluster_element_completed({
                endDate: '2026-10-23T10:00:05Z',
                name: 'httpClient_1',
                startDate: '2026-10-23T10:00:04Z',
                status: 'COMPLETED',
                taskExecutionId: '11',
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenLastCalledWith(
            'httpClient_1',
            expect.objectContaining({status: 'FAILED'})
        );
    });

    it('should mark a cluster element failed with its error', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.cluster_element_failed(
                JSON.stringify({
                    endDate: '2026-10-23T10:00:00.031Z',
                    error: 'boom',
                    name: 'httpClient_1',
                    startDate: '2026-10-23T10:00:00Z',
                    status: 'FAILED',
                    taskExecutionId: '10',
                })
            );
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenLastCalledWith('httpClient_1', {
            clusterElement: true,
            durationMillis: 31,
            error: 'boom',
            status: 'FAILED',
        });
    });

    it('should clear a cluster element left running when the run is stopped', () => {
        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.cluster_element_started({
                name: 'httpClient_1',
                startDate: '2026-10-23T10:00:00Z',
                status: 'STARTED',
                taskExecutionId: '10',
            });
        });

        mockWorkflowTestNodeStates = {httpClient_1: {status: 'RUNNING'}};

        act(() => {
            result.current.close();
        });

        expect(mockRemoveWorkflowTestNodeState).toHaveBeenCalledWith('httpClient_1');
    });

    it('should ignore task events without a name', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_started({taskExecutionId: '10'});
        });

        expect(mockSetWorkflowTestNodeState).not.toHaveBeenCalled();
    });

    it('should clear nodes left running when the job failed', () => {
        mockWorkflowTestNodeStates = {firecrawl_6: {status: 'RUNNING'}, firecrawl_7: {status: 'FAILED'}};

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {status: 'FAILED'}});
        });

        expect(mockRemoveWorkflowTestNodeState).toHaveBeenCalledWith('firecrawl_6');
        expect(mockRemoveWorkflowTestNodeState).not.toHaveBeenCalledWith('firecrawl_7');
        expect(mockSetWorkflowTestNodeState).not.toHaveBeenCalledWith('firecrawl_6', expect.anything());
    });

    it('should mark nodes left running as completed when the job completed', () => {
        mockWorkflowTestNodeStates = {task_1: {status: 'RUNNING'}};

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {status: 'COMPLETED'}});
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'COMPLETED'});
        expect(mockRemoveWorkflowTestNodeState).not.toHaveBeenCalled();
    });

    it('should clear a cluster element still running when the job completed instead of completing it', () => {
        mockWorkflowTestNodeStates = {
            subagent_1: {clusterElement: true, status: 'RUNNING'},
            task_1: {status: 'RUNNING'},
        };

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.result({job: {status: 'COMPLETED', taskExecutions: []}});
        });

        expect(mockRemoveWorkflowTestNodeState).toHaveBeenCalledWith('subagent_1');
        expect(mockSetWorkflowTestNodeState).not.toHaveBeenCalledWith(
            'subagent_1',
            expect.objectContaining({status: 'COMPLETED'})
        );
        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'COMPLETED'});
    });

    it('should backfill task dispatchers nested in a loop iteration or a condition branch', () => {
        mockWorkflowTestNodeStates = {condition_2: {status: 'COMPLETED'}, logger_1: {status: 'COMPLETED'}};

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.result({
                job: {
                    id: '1',
                    priority: 0,
                    status: 'COMPLETED',
                    taskExecutions: [
                        {
                            endDate: '2026-09-28T07:14:11.054Z',
                            iterations: [
                                [
                                    {
                                        children: [
                                            {
                                                endDate: '2026-09-28T07:14:11.001Z',
                                                startDate: '2026-09-28T07:14:11.001Z',
                                                status: 'COMPLETED',
                                                workflowTask: {name: 'condition_2', type: 'condition/v1'},
                                            },
                                        ],
                                        endDate: '2026-09-28T07:14:11.002Z',
                                        startDate: '2026-09-28T07:14:11.001Z',
                                        status: 'COMPLETED',
                                        workflowTask: {name: 'condition_1', type: 'condition/v1'},
                                    },
                                ],
                            ],
                            startDate: '2026-09-28T07:14:11.000Z',
                            status: 'COMPLETED',
                            workflowTask: {name: 'loop_1', type: 'loop/v1'},
                        },
                    ],
                },
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('loop_1', {durationMillis: 54, status: 'COMPLETED'});
        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('condition_1', {
            durationMillis: 1,
            status: 'COMPLETED',
        });
        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('condition_2', {
            durationMillis: 0,
            status: 'COMPLETED',
        });
        expect(mockSetWorkflowTestNodeState).not.toHaveBeenCalledWith('logger_1', expect.anything());
    });

    it('should take the durations a finished result adds up over repeated runs', () => {
        mockWorkflowTestNodeStates = {
            aiAgent_1: {durationMillis: 20, status: 'COMPLETED'},
            httpClient_1: {clusterElement: true, durationMillis: 5, error: 'boom', status: 'FAILED'},
        };

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        const agentTaskExecution = (startSecond: number, toolMillis: number, toolStatus: string) => ({
            clusterElementCalls: [
                {
                    endDate: `2026-09-28T07:14:0${startSecond}.${String(toolMillis).padStart(3, '0')}Z`,
                    name: 'httpClient_1',
                    startDate: `2026-09-28T07:14:0${startSecond}.000Z`,
                    status: toolStatus,
                },
            ],
            endDate: `2026-09-28T07:14:0${startSecond}.020Z`,
            startDate: `2026-09-28T07:14:0${startSecond}.000Z`,
            status: 'COMPLETED',
            workflowTask: {name: 'aiAgent_1', type: 'aiAgent/v1/chat'},
        });

        act(() => {
            eventHandlers.result({
                job: {
                    id: '1',
                    priority: 0,
                    status: 'COMPLETED',
                    taskExecutions: [
                        {
                            endDate: '2026-09-28T07:14:03.000Z',
                            iterations: [
                                [agentTaskExecution(1, 10, 'COMPLETED')],
                                [agentTaskExecution(2, 5, 'FAILED')],
                            ],
                            startDate: '2026-09-28T07:14:00.000Z',
                            status: 'COMPLETED',
                            workflowTask: {name: 'loop_1', type: 'loop/v1'},
                        },
                    ],
                },
            });
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('aiAgent_1', {
            durationMillis: 40,
            status: 'COMPLETED',
        });
        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('httpClient_1', {
            clusterElement: true,
            durationMillis: 15,
            error: 'boom',
            status: 'FAILED',
        });
    });

    it('should show a progress snapshot while the workflow is running', () => {
        mockWorkflowIsRunning = true;

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.progress(
                JSON.stringify({job: {id: '1', priority: 0, status: 'STARTED', taskExecutions: []}})
            );
        });

        expect(mockSetWorkflowTestExecution).toHaveBeenCalledWith(
            expect.objectContaining({job: expect.objectContaining({id: '1', status: 'STARTED'})})
        );
        expect(mockSetWorkflowIsRunning).not.toHaveBeenCalledWith(false);
    });

    it('should ignore a progress snapshot that arrives after the run ended', () => {
        mockWorkflowIsRunning = false;

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.progress({job: {id: '1', priority: 0, status: 'STARTED'}});
        });

        expect(mockSetWorkflowTestExecution).not.toHaveBeenCalled();
    });

    it('should drop a progress snapshot on close so a stopped run does not read as running', () => {
        mockWorkflowTestExecution = {job: {status: 'STARTED'}};

        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        act(() => {
            result.current.close();
        });

        expect(mockSetWorkflowTestExecution).toHaveBeenCalledWith(undefined);
    });

    it('should keep a finished result on close', () => {
        mockWorkflowTestExecution = {job: {status: 'COMPLETED'}};

        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        act(() => {
            result.current.close();
        });

        expect(mockSetWorkflowTestExecution).not.toHaveBeenCalled();
    });

    it('should clear the stream request and call onStreamEnd when the stream closes', () => {
        const onStreamEnd = vi.fn();

        const {result} = renderHook(() => useWorkflowTestStream({onStreamEnd, workflowId: 'workflow-123'}));

        const mockRequest = {init: {method: 'POST'}, url: '/api/platform/internal/workflow-tests'};

        act(() => {
            result.current.setStreamRequest(mockRequest);
        });

        expect(useSSE).toHaveBeenLastCalledWith(mockRequest, expect.any(Object));

        const {onClose} = (useSSE as any).mock.lastCall[1];

        act(() => {
            onClose();
        });

        expect(onStreamEnd).toHaveBeenCalledTimes(1);
        expect(useSSE).toHaveBeenLastCalledWith(null, expect.any(Object));
    });

    it('should stop the run when the stream closes without a result and no onStreamEnd is given', () => {
        const streamRequest = {init: {method: 'POST'}, url: '/test'};

        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        act(() => {
            result.current.setStreamRequest(streamRequest);
        });

        const {onClose} = (useSSE as any).mock.lastCall[1];

        act(() => {
            onClose();
        });

        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
    });

    it('should show the question and keep the run going when the agent asks the user a question', () => {
        const onStreamEnd = vi.fn();
        const streamRequest = {init: {method: 'POST'}, url: '/test'};

        const {result} = renderHook(() => useWorkflowTestStream({onStreamEnd, workflowId: 'workflow-123'}));

        act(() => {
            result.current.setStreamRequest(streamRequest);
        });

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.ask_user_question({
                questions: [{header: 'Color', multiSelect: false, options: [], question: 'Which color?'}],
            });
        });

        expect(onStreamEnd).not.toHaveBeenCalled();
        expect(mockSetWorkflowIsRunning).not.toHaveBeenCalledWith(false);
        expect(useSSE).toHaveBeenLastCalledWith(streamRequest, expect.any(Object));
    });

    it('should keep a question asked during the run when the run ends with a final message', () => {
        useWorkflowTestChatStore.setState({messages: [{content: '', role: 'assistant'}]});

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.start({jobId: 42});
            eventHandlers.ask_user_question({
                questions: [{header: 'Color', multiSelect: false, options: [], question: 'Which color?'}],
            });
            eventHandlers.stream('Answer in your next message.');
            eventHandlers.result({job: {outputs: {message: 'Answer in your next message.'}, status: 'COMPLETED'}});
        });

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1].content).toEqual(expect.stringContaining('**Color**: Which color?'));
        expect(mockSetWorkflowIsRunning).toHaveBeenCalledWith(false);
        expect(mockPersistJobId).toHaveBeenLastCalledWith(null);
    });

    it('should show the final message of a run that follows a run in which a question was asked', () => {
        useWorkflowTestChatStore.setState({messages: [{content: '', role: 'assistant'}]});

        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const eventHandlers = (useSSE as any).mock.lastCall[1].eventHandlers;

        act(() => {
            eventHandlers.start({jobId: 1});
            eventHandlers.ask_user_question({
                questions: [{header: 'Color', multiSelect: false, options: [], question: 'Which color?'}],
            });
            eventHandlers.result({job: {outputs: {message: 'First'}, status: 'COMPLETED'}});
        });

        act(() => {
            useWorkflowTestChatStore.getState().setMessage({content: 'Again', role: 'user'});
            useWorkflowTestChatStore.getState().setMessage({content: '', role: 'assistant'});
        });

        act(() => {
            eventHandlers.start({jobId: 2});
            eventHandlers.result({job: {outputs: {message: 'Second'}, status: 'COMPLETED'}});
        });

        const {messages} = useWorkflowTestChatStore.getState();

        expect(messages[messages.length - 1].content).toBe('Second');
    });

    it('should ignore a stream close when no onStreamEnd is given', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        const {onClose} = (useSSE as any).mock.calls[0][1];

        expect(() => act(() => onClose())).not.toThrow();
    });
});
