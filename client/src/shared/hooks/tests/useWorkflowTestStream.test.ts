import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
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
