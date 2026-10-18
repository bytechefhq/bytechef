import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubChatComposer from '../AiHubChatComposer';

const {cancelAiHubRunMock, cancelRunMock, composerTextRef, currentWorkspaceIdRef, setTextMock} = vi.hoisted(() => ({
    cancelAiHubRunMock: vi.fn(),
    cancelRunMock: vi.fn(),
    composerTextRef: {current: ''},
    currentWorkspaceIdRef: {current: 3 as number | undefined},
    setTextMock: vi.fn(),
}));

vi.mock('@assistant-ui/react', () => ({
    ComposerPrimitive: {
        Input: (props: Record<string, unknown>) => <textarea {...props} />,
        Root: ({children}: {children: ReactNode}) => <form data-testid="composer-root">{children}</form>,
        Send: ({children}: {children: ReactNode}) => <>{children}</>,
    },
    useAui: () => ({
        composer: {
            getState: () => ({text: composerTextRef.current}),
            setText: setTextMock,
        },
        thread: {cancelRun: cancelRunMock},
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCancelAiHubRunMutation: () => ({mutate: cancelAiHubRunMock}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number | undefined}) => unknown) =>
        selector({currentWorkspaceId: currentWorkspaceIdRef.current}),
}));

const PENDING_APPROVAL_MESSAGE = {
    content: [
        {
            data: {approvalId: 4, awaitingApproval: true, kind: 'tool-approval-request', toolName: 'deleteWorkflow'},
            type: 'data-tool-approval-request' as const,
        },
    ],
    role: 'assistant' as const,
};

const renderComposer = (modelPicker?: ReactNode) =>
    render(
        <TooltipProvider>
            <AiHubChatComposer modelPicker={modelPicker} />
        </TooltipProvider>
    );

describe('AiHubChatComposer', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        composerTextRef.current = '';
        currentWorkspaceIdRef.current = 3;

        aiHubStore.setState({chatId: 'thread-7', messages: [], mode: MODE.BUILD});
        aiHubChatsStore.setState({currentChatId: 7});
        aiHubRunStateStore.setState({runIdByChat: {}, runningByChat: {}});
    });

    it('renders the input, the model picker slot, the mode switch and Send when idle', () => {
        renderComposer(<div data-testid="model-picker" />);

        expect(screen.getByRole('textbox', {name: 'Message input'})).toBeEnabled();
        expect(screen.getByTestId('model-picker')).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Build mode on'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Send message'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Stop generating'})).toBeNull();
    });

    it.each([undefined, 0])('disables the input and Send while the workspace id is %s', (workspaceId) => {
        currentWorkspaceIdRef.current = workspaceId;

        renderComposer();

        expect(screen.getByRole('textbox', {name: 'Message input'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Send message'})).toBeDisabled();
    });

    it('switches between Build and Ask mode', () => {
        renderComposer();

        fireEvent.click(screen.getByRole('switch', {name: 'Build mode on'}));

        expect(aiHubStore.getState().mode).toBe(MODE.ASK);

        fireEvent.click(screen.getByRole('switch', {name: 'Build mode off (Ask)'}));

        expect(aiHubStore.getState().mode).toBe(MODE.BUILD);
    });

    it('disables the input and swaps Send for Stop while a turn runs', () => {
        aiHubRunStateStore.setState({runningByChat: {'thread-7': true}});

        renderComposer();

        expect(screen.getByRole('textbox', {name: 'Message input'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Send message'})).toBeNull();
        expect(screen.getByRole('button', {name: 'Stop generating'})).toBeInTheDocument();
    });

    it('re-enables the input once the turn settles', () => {
        aiHubRunStateStore.setState({runningByChat: {'thread-7': true}});

        renderComposer();

        act(() => {
            aiHubRunStateStore.getState().setChatRunning('thread-7', false);
        });

        expect(screen.getByRole('textbox', {name: 'Message input'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Send message'})).toBeInTheDocument();
    });

    it('ignores a run of another chat', () => {
        aiHubRunStateStore.setState({runningByChat: {'thread-other': true}});

        renderComposer();

        expect(screen.getByRole('textbox', {name: 'Message input'})).toBeEnabled();
    });

    it('Stop cancels the client stream and calls cancelAiHubRun with the run id', () => {
        aiHubRunStateStore.setState({runIdByChat: {'thread-7': 'run-1'}, runningByChat: {'thread-7': true}});

        renderComposer();

        fireEvent.click(screen.getByRole('button', {name: 'Stop generating'}));

        expect(cancelRunMock).toHaveBeenCalledTimes(1);
        expect(cancelAiHubRunMock).toHaveBeenCalledWith({id: '7', runId: 'run-1', workspaceId: '3'});
    });

    it('Stop on the home view only cancels the client stream', () => {
        aiHubChatsStore.setState({currentChatId: undefined});
        aiHubRunStateStore.setState({runningByChat: {'thread-7': true}});

        renderComposer();

        fireEvent.click(screen.getByRole('button', {name: 'Stop generating'}));

        expect(cancelRunMock).toHaveBeenCalledTimes(1);
        expect(cancelAiHubRunMock).not.toHaveBeenCalled();
    });

    it('says that sending withdraws a pending tool approval', () => {
        aiHubStore.setState({messages: [PENDING_APPROVAL_MESSAGE]});

        renderComposer();

        const input = screen.getByRole('textbox', {name: 'Message input'});

        expect(input).toBeEnabled();
        expect(input).toHaveAttribute(
            'placeholder',
            'Approve or reject the tool call above, or send a message to withdraw it...'
        );
    });

    it('uses the default placeholder once the approval has been decided', () => {
        aiHubStore.setState({
            messages: [
                {
                    ...PENDING_APPROVAL_MESSAGE,
                    content: [
                        {
                            ...PENDING_APPROVAL_MESSAGE.content[0],
                            data: {...PENDING_APPROVAL_MESSAGE.content[0].data, resolvedStatus: 'APPROVED'},
                        },
                    ],
                },
            ],
        });

        renderComposer();

        expect(screen.getByRole('textbox', {name: 'Message input'})).toHaveAttribute(
            'placeholder',
            'Send a message...'
        );
    });

    it('clears an unsent draft when the chat changes, but not on first render', () => {
        composerTextRef.current = 'draft';

        renderComposer();

        expect(setTextMock).not.toHaveBeenCalled();

        act(() => {
            aiHubStore.setState({chatId: 'thread-8'});
        });

        expect(setTextMock).toHaveBeenCalledWith('');
    });
});
