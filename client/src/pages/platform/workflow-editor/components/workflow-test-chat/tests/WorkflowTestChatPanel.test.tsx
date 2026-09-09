import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/components/assistant-ui/thread', () => ({
    Thread: () => <div>Thread</div>,
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider',
    () => ({
        WorkflowTestChatRuntimeProvider: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    })
);

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
}));

const renderPanel = () =>
    render(
        <TooltipProvider>
            <WorkflowTestChatPanel />
        </TooltipProvider>
    );

beforeEach(() => {
    windowResizeObserver();

    useWorkflowTestChatStore.setState({
        conversationId: undefined,
        messages: [],
        resumeUrl: null,
        workflowTestChatPanelOpen: true,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowTestChatPanel', () => {
    it('renders nothing while the panel is closed', () => {
        useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: false});

        renderPanel();

        expect(screen.queryByText('Playground')).not.toBeInTheDocument();
    });

    it('clears the conversation and starts a new one when reset', async () => {
        renderPanel();

        useWorkflowTestChatStore.setState({
            messages: [{content: 'Hello', role: 'user'}],
            resumeUrl: 'https://example.com/resume',
        });

        const conversationIdBeforeReset = useWorkflowTestChatStore.getState().conversationId;

        await userEvent.click(screen.getByRole('button', {name: 'Reset the conversation'}));

        const state = useWorkflowTestChatStore.getState();

        expect(state.messages).toEqual([]);
        expect(state.resumeUrl).toBeNull();
        expect(state.conversationId).not.toBe(conversationIdBeforeReset);
    });

    it('closes the panel', async () => {
        renderPanel();

        await userEvent.click(screen.getByRole('button', {name: 'Close the playground panel'}));

        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(false);
    });
});
