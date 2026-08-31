import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubThread', () => ({
    default: () => <div data-testid="cc-thread" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: () => <div data-testid="cc-composer" />,
}));

// vi.mock factories hoist above module-scope consts, so the per-test-configurable chats list has to be
// declared via vi.hoisted rather than a plain outer `let` — see AiHubChatsSidebar.test.tsx for the same
// pattern.
const {mockChatsDataRef} = vi.hoisted(() => ({
    mockChatsDataRef: {current: [] as AiHubChatI[]},
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatArtifactsQuery: () => ({data: []}),
    useAiHubChatsQuery: () => ({data: mockChatsDataRef.current, isLoading: false}),
}));

afterEach(() => {
    mockChatsDataRef.current = [];
    aiHubChatsStore.getState().reset();
});

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        aiAgentId: null,
        autoTitled: true,
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'STANDARD',
        lastPreview: null,
        messageCount: 0,
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: null,
        updatedAt: new Date().toISOString(),
        userId: 1,
        workflowExecutionId: null,
        workspaceId: 1049,
        ...overrides,
    };
}

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}},
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <TooltipProvider>{ui}</TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('AiHubPanel', () => {
    it('renders the chat title (New Chat fallback) and the thread + composer', () => {
        wrap(<AiHubPanel />);

        // Header now shows the chat's title (or "New Chat" when no title yet) — the
        // legacy header text and the bot icon were removed when the runtime provider was hoisted to
        // AiHubContent. The Ask/Build control moved out of the header into the composer (ModeSwitch),
        // which is mocked here, so it's asserted in the composer's own tests rather than this one.
        expect(screen.getByText('New Chat')).toBeInTheDocument();
        expect(screen.getByTestId('cc-thread')).toBeInTheDocument();
        expect(screen.getByTestId('cc-composer')).toBeInTheDocument();
    });

    it('does not render the global SubagentProgressLine — subagent progress is rendered per tool-call card', () => {
        const {container} = wrap(<AiHubPanel />);

        expect(container.querySelector('.italic')).toBeNull();
    });

    it('fills its container (no hardcoded w-[450px])', () => {
        const {container} = wrap(<AiHubPanel />);

        expect(container.querySelector('.w-\\[450px\\]')).toBeNull();
        expect(container.querySelector('.size-full')).not.toBeNull();
    });
});

describe('AiHubPanel channel-born agent chat badge', () => {
    // A channel-born AGENT_CHAT (recorded from the agent's Slack/schedule run: aiAgentId stamped,
    // workflowExecutionId left null) must read differently from a composer-created one in the header
    // badge too, not just the sidebar row — otherwise the one place the panel names the chat's identity
    // says "Agent chat" for both, hiding that this conversation didn't start in AI Hub at all. Both
    // signals matter — see isChannelAgentChat and the mirrored server guard
    // AiHubAgentConversationRecorder#adoptChat — so these tests exercise the discriminator's corners, not
    // just today's two "normal" shapes.
    it('labels a channel-born agent chat "Agent conversation" with its own generic title', () => {
        mockChatsDataRef.current = [
            buildChat({aiAgentId: 9, id: 31, kind: 'AGENT_CHAT', title: null, workflowExecutionId: null}),
        ];
        aiHubChatsStore.getState().setCurrentChatId(31);

        wrap(<AiHubPanel />);

        expect(screen.getByText('Agent conversation')).toBeInTheDocument();
        expect(screen.getByText('Agent Conversation')).toBeInTheDocument();
        expect(screen.queryByText('Agent chat')).not.toBeInTheDocument();
    });

    // Baseline regression guard, not new-behaviour coverage: this exact (aiAgentId null,
    // workflowExecutionId non-null) shape is today's actual composer-created row, and "Agent chat" was
    // already the label for every AGENT_CHAT row before the channel-born distinction existed. It passes
    // identically with or without isChannelAgentChat. Kept because the badge logic still routes through
    // it — a revert that broke this fallback branch would still be worth catching here.
    it('labels a composer-created agent chat "Agent chat" (baseline, unchanged by the channel-born distinction)', () => {
        mockChatsDataRef.current = [
            buildChat({aiAgentId: null, id: 32, kind: 'AGENT_CHAT', title: null, workflowExecutionId: 'exec-1'}),
        ];
        aiHubChatsStore.getState().setCurrentChatId(32);

        wrap(<AiHubPanel />);

        expect(screen.getByText('Agent chat')).toBeInTheDocument();
        expect(screen.queryByText('Agent conversation')).not.toBeInTheDocument();
        expect(screen.queryByText('Agent Conversation')).not.toBeInTheDocument();
    });

    it('labels a hypothetical aiAgentId-stamped-but-bound row "Agent chat", not "Agent conversation" (both signals required)', () => {
        // A single-signal ("aiAgentId != null") discriminator would misclassify this row as channel-born.
        // This combination doesn't occur in practice today (the recorder never sets
        // workflowExecutionId), but the discriminator must not rely on aiAgentId alone any more than it
        // may rely on workflowExecutionId alone — this pins the corner nothing else here does.
        mockChatsDataRef.current = [
            buildChat({aiAgentId: 9, id: 33, kind: 'AGENT_CHAT', title: null, workflowExecutionId: 'exec-1'}),
        ];
        aiHubChatsStore.getState().setCurrentChatId(33);

        wrap(<AiHubPanel />);

        expect(screen.getByText('Agent chat')).toBeInTheDocument();
        expect(screen.queryByText('Agent conversation')).not.toBeInTheDocument();
        expect(screen.queryByText('Agent Conversation')).not.toBeInTheDocument();
    });
});
