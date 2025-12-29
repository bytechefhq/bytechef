import {useSSE} from '@/shared/hooks/useSSE';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowTestStream} from '../useWorkflowTestStream';

const mockSetWorkflowIsRunning = vi.fn();
const mockSetWorkflowTestExecution = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: vi.fn((selector) =>
        selector({
            setWorkflowIsRunning: mockSetWorkflowIsRunning,
            setWorkflowTestExecution: mockSetWorkflowTestExecution,
        })
    ),
    useWorkflowEditorStore: vi.fn((selector) =>
        selector({
            setWorkflowIsRunning: mockSetWorkflowIsRunning,
            setWorkflowTestExecution: mockSetWorkflowTestExecution,
        })
    ),
}));

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
});
