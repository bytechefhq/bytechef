import {useSSE} from '@/shared/hooks/useSSE';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowTestStream} from '../useWorkflowTestStream';

const mockResetWorkflowTestNodeStates = vi.fn();
const mockSetWorkflowIsRunning = vi.fn();
const mockSetWorkflowTestExecution = vi.fn();
const mockSetWorkflowTestNodeState = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => {
    // Lazy: the mock* consts are hoisted below this factory, so they must only be dereferenced at call time.
    const getStoreState = () => ({
        resetWorkflowTestNodeStates: mockResetWorkflowTestNodeStates,
        setWorkflowIsRunning: mockSetWorkflowIsRunning,
        setWorkflowTestExecution: mockSetWorkflowTestExecution,
        setWorkflowTestNodeState: mockSetWorkflowTestNodeState,
        workflowTestNodeStates: {},
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

    it('should handle stream event with valid chunk', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.stream({text: 'streaming text'});
        });
    });

    it('should return close function from useSSE', () => {
        const {result} = renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        expect(result.current.close).toBe(mockClose);
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

    it('should reset node states on start event, scoped to the workflow that is running', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.start({jobId: 'job-123'});
        });

        expect(mockResetWorkflowTestNodeStates).toHaveBeenCalledWith('workflow-123');
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

    it('should handle task_started event with string data', () => {
        renderHook(() => useWorkflowTestStream({workflowId: 'workflow-123'}));

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const eventHandlers = (useSSE as any).mock.calls[0][1].eventHandlers;

        act(() => {
            eventHandlers.task_started('{"name":"task_1","taskExecutionId":"10"}');
        });

        expect(mockSetWorkflowTestNodeState).toHaveBeenCalledWith('task_1', {status: 'RUNNING'});
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
});
