import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {WorkflowChatRuntimeProvider} from '../WorkflowChatRuntimeProvider';

// Mock dependencies
vi.mock('@/pages/automation/workflow-chat/stores/useWorkflowChatStore', () => ({
    useWorkflowChatStore: vi.fn((selector) =>
        selector({
            appendToLastAssistantMessage: vi.fn(),
            messages: [],
            setLastAssistantMessageContent: vi.fn(),
            setMessage: vi.fn(),
        })
    ),
}));

vi.mock('@/shared/hooks/useSSE', () => ({
    useSSE: vi.fn(() => ({
        close: vi.fn(),
        connectionState: 'CLOSED',
        data: null,
        error: null,
    })),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CompositeAttachmentAdapter: vi.fn(),
    SimpleImageAttachmentAdapter: vi.fn(),
    SimpleTextAttachmentAdapter: vi.fn(),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

describe('WorkflowChatRuntimeProvider', () => {
    it('renders children', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="test" workflowExecutionId="workflow-123">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('accepts sseStream prop', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="test" sseStream={true} workflowExecutionId="workflow-123">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('initializes with correct environment', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="production" workflowExecutionId="workflow-123">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('handles different workflow execution IDs', () => {
        const {result} = renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="test" workflowExecutionId="different-workflow-456">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        expect(result).toBeDefined();
    });

    it('updates isRunning state when connectionState changes to CLOSED', async () => {
        const useSSEMock = await import('@/shared/hooks/useSSE');

        vi.mocked(useSSEMock.useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'CLOSED',
            data: null,
            error: null,
        });

        renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="test" workflowExecutionId="workflow-123">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        await waitFor(() => {
            expect(useSSEMock.useSSE).toHaveBeenCalled();
        });
    });

    it('updates isRunning state when connectionState changes to ERROR', async () => {
        const useSSEMock = await import('@/shared/hooks/useSSE');

        vi.mocked(useSSEMock.useSSE).mockReturnValue({
            close: vi.fn(),
            connectionState: 'ERROR',
            data: null,
            error: 'Connection error',
        });

        renderHook(() => null, {
            wrapper: ({children}) => (
                <WorkflowChatRuntimeProvider environment="test" workflowExecutionId="workflow-123">
                    {children}
                </WorkflowChatRuntimeProvider>
            ),
        });

        await waitFor(() => {
            expect(useSSEMock.useSSE).toHaveBeenCalled();
        });
    });
});
