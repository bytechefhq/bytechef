import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubChatsSidebar, {
    CHATS_PAGE_SIZE,
    cancelChatRunIfStreaming,
    getChatsPage,
    reconcileProbedChatActivity,
} from '../AiHubChatsSidebar';
import {AiHubChatI} from '../api/chats.api';

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: () => ({data: [], isLoading: false}),
    useDeleteAiHubChatMutation: () => ({mutate: vi.fn()}),
    usePatchAiHubChatMutation: () => ({mutate: vi.fn()}),
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat', () => ({
    useSwitchChat: () => vi.fn(),
}));
vi.mock('@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient', () => ({
    probeInFlightStatus: () => Promise.resolve({}),
}));
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));
vi.mock('@/shared/middleware/graphql', () => ({
    useCancelAiHubRunMutation: () => ({mutate: vi.fn()}),
    useCancelWorkflowChatTurnMutation: () => ({mutate: vi.fn()}),
}));

function renderSidebar() {
    return render(
        <MemoryRouter initialEntries={['/automation/ai-hub']}>
            <TooltipProvider>
                <AiHubChatsSidebar />
            </TooltipProvider>
        </MemoryRouter>
    );
}

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        aiHubTaskId: null,
        autoTitled: true,
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'STANDARD',
        lastPreview: null,
        messageCount: 0,
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: 'Chat',
        updatedAt: new Date().toISOString(),
        userId: 1,
        workflowExecutionId: null,
        workspaceId: 1,
        ...overrides,
    };
}

describe('cancelChatRunIfStreaming', () => {
    beforeEach(() => {
        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.setState({chatActivity: {}});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    function buildCancel() {
        return {cancelAiHubRun: vi.fn(), cancelWorkflowChatTurn: vi.fn()};
    }

    it('cancels the LLM run (with runId) and clears run state for a focused, streaming STANDARD chat', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-1', true);
        aiHubRunStateStore.getState().setChatRunId('thread-1', 'run-1');

        const cancel = buildCancel();

        cancelChatRunIfStreaming(buildChat({id: 7, kind: 'STANDARD', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).toHaveBeenCalledWith({id: '7', runId: 'run-1', workspaceId: '1'});
        expect(cancel.cancelWorkflowChatTurn).not.toHaveBeenCalled();
        expect(aiHubRunStateStore.getState().runningByChat['thread-1']).toBe(false);
    });

    it('cancels the workflow-chat turn for a streaming WORKFLOW_CHAT chat', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-1', true);

        const cancel = buildCancel();

        cancelChatRunIfStreaming(buildChat({id: 8, kind: 'WORKFLOW_CHAT', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelWorkflowChatTurn).toHaveBeenCalledWith({id: '8', workspaceId: '1'});
        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
    });

    it('treats a background chat with a probe-driven running activity state as streaming', () => {
        aiHubChatsStore.getState().setActivityState('thread-1', 'running');

        const cancel = buildCancel();

        cancelChatRunIfStreaming(buildChat({id: 9, kind: 'STANDARD', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).toHaveBeenCalledWith({id: '9', runId: undefined, workspaceId: '1'});
    });

    it('is a no-op when the chat is not streaming', () => {
        const cancel = buildCancel();

        cancelChatRunIfStreaming(buildChat({id: 10, threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
        expect(cancel.cancelWorkflowChatTurn).not.toHaveBeenCalled();
    });

    it('is a no-op when the workspace id is missing', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-1', true);

        const cancel = buildCancel();

        cancelChatRunIfStreaming(buildChat({id: 11, threadId: 'thread-1'}), undefined, cancel);

        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
    });
});

describe('reconcileProbedChatActivity', () => {
    it('does NOT re-set running for the focused thread when the probe still reports in-flight (just-cancelled race)', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: undefined,
            isFocused: true,
            isInFlight: true,
            isRunningByChat: false,
        });

        expect(decision.setActivityRunning).toBeFalsy();
    });

    it('sets running for a NON-focused (background) thread the probe reports in-flight with no activity yet', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: undefined,
            isFocused: false,
            isInFlight: true,
            isRunningByChat: false,
        });

        expect(decision.setActivityRunning).toBe(true);
    });

    it('preserves paused regardless of probe', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: 'paused',
            isFocused: false,
            isInFlight: false,
            isRunningByChat: true,
        });

        expect(decision).toEqual({});
    });

    it('clears a focused running dot once the runtime has released the chat (not in flight, runningByChat false)', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: 'running',
            isFocused: true,
            isInFlight: false,
            isRunningByChat: false,
        });

        expect(decision.clearActivity).toBe(true);
    });

    it('does not clear a focused running dot while the runtime still owns it (runningByChat true)', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: 'running',
            isFocused: true,
            isInFlight: false,
            isRunningByChat: true,
        });

        expect(decision).toEqual({});
    });

    it('clears both the running flag and the dot for a finished background chat', () => {
        const decision = reconcileProbedChatActivity({
            currentActivity: 'running',
            isFocused: false,
            isInFlight: false,
            isRunningByChat: true,
        });

        expect(decision.clearRunningFlag).toBe(true);
        expect(decision.clearActivity).toBe(true);
    });
});

describe('getChatsPage', () => {
    const chats = Array.from({length: CHATS_PAGE_SIZE * 2 + 5}, (_, index) => buildChat({id: index + 1}));

    it('returns all chats with no hidden count when the list fits within the visible window', () => {
        const {hiddenCount, visibleChats} = getChatsPage(chats.slice(0, 5), CHATS_PAGE_SIZE);

        expect(visibleChats).toHaveLength(5);
        expect(hiddenCount).toBe(0);
    });

    it('caps the visible chats at the window size and reports the remainder as hidden', () => {
        const {hiddenCount, visibleChats} = getChatsPage(chats, CHATS_PAGE_SIZE);

        expect(visibleChats).toHaveLength(CHATS_PAGE_SIZE);
        expect(hiddenCount).toBe(CHATS_PAGE_SIZE + 5);
    });

    it('reveals the next page after the window grows', () => {
        const {hiddenCount, visibleChats} = getChatsPage(chats, CHATS_PAGE_SIZE * 2);

        expect(visibleChats).toHaveLength(CHATS_PAGE_SIZE * 2);
        expect(hiddenCount).toBe(5);
    });

    it('handles an empty list', () => {
        const {hiddenCount, visibleChats} = getChatsPage([], CHATS_PAGE_SIZE);

        expect(visibleChats).toHaveLength(0);
        expect(hiddenCount).toBe(0);
    });
});

describe('AiHubChatsSidebar top menu', () => {
    beforeEach(() => {
        aiHubChatsStore.getState().reset();
    });

    it('does not render a "Workflow Chats" nav item — the composer provider popup launches those chats', () => {
        renderSidebar();

        expect(screen.queryByText('Workflow Chats')).not.toBeInTheDocument();
    });

    it('still renders New Chat, Scheduled, and More', () => {
        renderSidebar();

        expect(screen.getByText('New Chat')).toBeInTheDocument();
        expect(screen.getByText('Scheduled')).toBeInTheDocument();
        expect(screen.getByText('More')).toBeInTheDocument();
    });
});

describe('AiHubChatsSidebar "More" menu', () => {
    beforeEach(() => {
        aiHubChatsStore.getState().reset();
    });

    it('does not render the inline "Context" group', () => {
        renderSidebar();

        expect(screen.queryByText('Context')).not.toBeInTheDocument();
    });

    // The entries live in a dropdown anchored inside the sidebar, so each one is a `menuitem` that is
    // also a link (DropdownMenuItem asChild + Link) — the href still has to point at the canonical page.
    it('opens Connectors, Memories, and Skills in a popup menu when "More" is clicked', async () => {
        renderSidebar();

        expect(screen.queryByRole('menuitem', {name: /memories/i})).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('More'));

        expect(await screen.findByRole('menuitem', {name: /memories/i})).toHaveAttribute(
            'href',
            '/automation/ai/memories'
        );
        expect(screen.getByRole('menuitem', {name: /connectors/i})).toHaveAttribute(
            'href',
            '/automation/settings/ai-hub/connectors'
        );
        expect(screen.getByRole('menuitem', {name: /skills/i})).toHaveAttribute('href', '/automation/ai/skills');
    });

    it('closes the menu again when dismissed with Escape', async () => {
        renderSidebar();

        await userEvent.click(screen.getByText('More'));

        expect(await screen.findByRole('menuitem', {name: /memories/i})).toBeInTheDocument();

        await userEvent.keyboard('{Escape}');

        await waitFor(() => expect(screen.queryByRole('menuitem', {name: /memories/i})).not.toBeInTheDocument());
    });

    it('closes the menu again on a second click of "More"', async () => {
        // The open menu is modal: Radix puts `pointer-events: none` on the body, so in a real browser the
        // second click never reaches the trigger — the topmost element at those coordinates is <html>, and
        // Radix's dismiss-on-outside-pointerdown closes the menu (verified in Chrome). user-event refuses
        // to dispatch onto a pointer-events:none target at all, so the check is disabled to exercise that
        // same outside-pointerdown dismissal instead of asserting jsdom's guard.
        const user = userEvent.setup({pointerEventsCheck: 0});

        renderSidebar();

        await user.click(screen.getByText('More'));

        expect(await screen.findByRole('menuitem', {name: /memories/i})).toBeInTheDocument();

        await user.click(screen.getByText('More'));

        await waitFor(() => expect(screen.queryByRole('menuitem', {name: /memories/i})).not.toBeInTheDocument());
    });
});
