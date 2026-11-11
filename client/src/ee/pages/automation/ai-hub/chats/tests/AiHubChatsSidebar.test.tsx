import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubChatsSidebar, {CHATS_PAGE_SIZE, getChatsPage} from '../AiHubChatsSidebar';
import {AiHubChatI} from '../api/chats.api';

const {
    generateAiHubChatTitleMock,
    liveWorkflowLabelRef,
    mockChatsDataRef,
    mockChatsLoadingRef,
    patchChatMock,
    switchChatMock,
    useAiHubChatsQueryMock,
} = vi.hoisted(() => ({
    generateAiHubChatTitleMock: vi.fn(),
    liveWorkflowLabelRef: {current: null as string | null},
    mockChatsDataRef: {current: [] as AiHubChatI[]},
    mockChatsLoadingRef: {current: false},
    patchChatMock: vi.fn(),
    switchChatMock: vi.fn(),
    useAiHubChatsQueryMock: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    AiHubChatsKeys: {all: ['aiHubChats']},
    useAiHubChatsQuery: (...args: unknown[]) => {
        useAiHubChatsQueryMock(...args);

        return {data: mockChatsDataRef.current, isLoading: mockChatsLoadingRef.current};
    },
    useDeleteAiHubChatMutation: () => ({mutate: vi.fn()}),
    usePatchAiHubChatMutation: () => ({mutate: patchChatMock}),
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useLiveWorkflowLabel', () => ({
    useLiveWorkflowLabel: () => liveWorkflowLabelRef.current,
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat', () => ({
    useSwitchChat: () => switchChatMock,
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/ee/pages/automation/ai-hub/chats/api/chats.api')>();

    return {...actual, generateAiHubChatTitle: generateAiHubChatTitleMock};
});
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));
vi.mock('@/shared/middleware/graphql', () => ({
    useCancelAiHubRunMutation: () => ({mutate: vi.fn()}),
}));

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        autoTitled: true,
        createdAt: new Date().toISOString(),
        environmentId: 2,
        id: 1,
        isOwner: true,
        kind: 'STANDARD',
        lastPreview: null,
        messageCount: 0,
        ownerUserId: 1,
        projectDeploymentId: null,
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

function renderSidebar(path = '/automation/ai-hub') {
    return render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <MemoryRouter initialEntries={[path]}>
                <TooltipProvider>
                    <AiHubChatsSidebar />
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
}

beforeEach(() => {
    aiHubChatsStore.getState().reset();
});

afterEach(() => {
    liveWorkflowLabelRef.current = null;
    mockChatsDataRef.current = [];
    mockChatsLoadingRef.current = false;
    vi.clearAllMocks();
});

describe('getChatsPage', () => {
    const chats = Array.from({length: 30}, (_, index) => index);

    it('returns all chats with no hidden count when the list fits within the visible window', () => {
        expect(getChatsPage(chats.slice(0, 5), CHATS_PAGE_SIZE)).toEqual({
            hiddenCount: 0,
            visibleChats: [0, 1, 2, 3, 4],
        });
    });

    it('caps the visible chats at the window size and reports the remainder as hidden', () => {
        const page = getChatsPage(chats, CHATS_PAGE_SIZE);

        expect(page.visibleChats).toHaveLength(CHATS_PAGE_SIZE);
        expect(page.hiddenCount).toBe(30 - CHATS_PAGE_SIZE);
    });

    it('handles an empty list', () => {
        expect(getChatsPage([], CHATS_PAGE_SIZE)).toEqual({hiddenCount: 0, visibleChats: []});
    });
});

describe('AiHubChatsSidebar', () => {
    it('renders a workflow icon for a workflow chat and the live workflow label in its tooltip', async () => {
        liveWorkflowLabelRef.current = 'Acme — Support v2';
        mockChatsDataRef.current = [
            buildChat({id: 5, kind: 'WORKFLOW_CHAT', title: 'Acme — Support', workflowExecutionId: 'exec-1'}),
        ];

        renderSidebar();

        const workflowIcon = screen.getByRole('img', {name: 'Workflow chat'});

        await userEvent.hover(workflowIcon);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Current workflow label: Acme — Support v2');
    });

    it('renders no workflow icon for a standard chat', () => {
        mockChatsDataRef.current = [buildChat({id: 5, title: 'Plain'})];

        renderSidebar();

        expect(screen.queryByRole('img', {name: 'Workflow chat'})).toBeNull();
    });

    it('renders New Chat and nothing from the dropped menus', () => {
        renderSidebar();

        expect(screen.getByRole('link', {name: 'New Chat'})).toHaveAttribute('href', '/automation/ai-hub');
        expect(screen.queryByText('Scheduled')).toBeNull();
        expect(screen.queryByText('More')).toBeNull();
        expect(screen.queryByText('Shared with me')).toBeNull();
    });

    it('lists the active chats of the current workspace and environment', () => {
        mockChatsDataRef.current = [buildChat({id: 1, title: 'First'}), buildChat({id: 2, title: 'Second'})];

        renderSidebar();

        expect(useAiHubChatsQueryMock).toHaveBeenCalledWith(1, 2, 'ACTIVE');
        expect(screen.getByText('First')).toBeInTheDocument();
        expect(screen.getByText('Second')).toBeInTheDocument();
    });

    it('shows the empty state', () => {
        renderSidebar();

        expect(screen.getByText('No chats yet.')).toBeInTheDocument();
    });

    it('switches to a chat when its row is clicked', () => {
        const chat = buildChat({id: 3, title: 'Pick me'});

        mockChatsDataRef.current = [chat];

        renderSidebar();

        fireEvent.click(screen.getByText('Pick me'));

        expect(switchChatMock).toHaveBeenCalledWith(chat);
    });

    it('filters by title and preview', async () => {
        const user = userEvent.setup();

        mockChatsDataRef.current = [
            buildChat({id: 1, title: 'Invoices'}),
            buildChat({id: 2, lastPreview: 'about invoices', title: 'Other'}),
            buildChat({id: 3, title: 'Leads'}),
        ];

        renderSidebar();

        await user.type(screen.getByPlaceholderText('Search chats...'), 'invoice');

        expect(screen.getByText('Invoices')).toBeInTheDocument();
        expect(screen.getByText('Other')).toBeInTheDocument();
        expect(screen.queryByText('Leads')).toBeNull();
    });

    it('switches to the archived list and back', async () => {
        const user = userEvent.setup();

        renderSidebar();

        await user.click(screen.getByRole('button', {name: 'Archive'}));

        expect(aiHubChatsStore.getState().activeFilter).toBe('ARCHIVED');
        expect(useAiHubChatsQueryMock).toHaveBeenLastCalledWith(1, 2, 'ARCHIVED');
        expect(screen.getByText('No archived chats yet.')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Back to active chats'}));

        expect(aiHubChatsStore.getState().activeFilter).toBe('ACTIVE');
    });

    it('titles the list Archived chats in the archive view and Chats otherwise', async () => {
        const user = userEvent.setup();

        renderSidebar();

        expect(screen.getByRole('heading', {level: 4})).toHaveTextContent(/^Chats$/);

        await user.click(screen.getByRole('button', {name: 'Archive'}));

        expect(screen.getByRole('heading', {level: 4})).toHaveTextContent(/^Archived chats$/);

        await user.click(screen.getByRole('button', {name: 'Back to active chats'}));

        expect(screen.getByRole('heading', {level: 4})).toHaveTextContent(/^Chats$/);
    });

    it('pages the list', async () => {
        const user = userEvent.setup();

        mockChatsDataRef.current = Array.from({length: CHATS_PAGE_SIZE + 3}, (_, index) =>
            buildChat({id: index + 1, threadId: `thread-${index + 1}`, title: `Chat ${index + 1}`})
        );

        renderSidebar();

        expect(screen.queryByText(`Chat ${CHATS_PAGE_SIZE + 1}`)).toBeNull();

        await user.click(screen.getByRole('button', {name: 'Show 3 more'}));

        expect(screen.getByText(`Chat ${CHATS_PAGE_SIZE + 3}`)).toBeInTheDocument();
    });

    it('marks running and paused chats', () => {
        mockChatsDataRef.current = [
            buildChat({id: 1, threadId: 'thread-1', title: 'Busy'}),
            buildChat({id: 2, threadId: 'thread-2', title: 'Waiting'}),
        ];

        aiHubChatsStore.setState({chatActivity: {'thread-1': 'running', 'thread-2': 'paused'}});

        renderSidebar();

        expect(screen.getByLabelText('Chat is running')).toBeInTheDocument();
        expect(screen.getByLabelText('Chat is waiting for your answer or approval')).toBeInTheDocument();
    });

    it('offers rename, archive and delete in the row menu, and archives from it', async () => {
        const user = userEvent.setup();

        mockChatsDataRef.current = [buildChat({id: 5, title: 'Row'})];

        renderSidebar();

        await user.click(screen.getByRole('button', {name: 'Chat actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Rename'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();

        await user.click(screen.getByRole('menuitem', {name: 'Archive'}));

        expect(patchChatMock).toHaveBeenCalledWith({chatId: 5, patch: {status: 'ARCHIVED'}, workspaceId: 1});
    });

    it('offers unarchive for an archived chat', async () => {
        const user = userEvent.setup();

        aiHubChatsStore.setState({activeFilter: 'ARCHIVED'});
        mockChatsDataRef.current = [buildChat({id: 5, status: 'ARCHIVED', title: 'Old'})];

        renderSidebar();

        await user.click(screen.getByRole('button', {name: 'Chat actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Unarchive'}));

        expect(patchChatMock).toHaveBeenCalledWith({chatId: 5, patch: {status: 'ACTIVE'}, workspaceId: 1});
    });

    it('retries a failed title generation and clears the failure on success', async () => {
        generateAiHubChatTitleMock.mockResolvedValue(buildChat({id: 5}));

        mockChatsDataRef.current = [buildChat({id: 5, title: null})];

        aiHubChatsStore.getState().markTitleGenerationFailed(5, 'model offline');

        renderSidebar();

        fireEvent.click(screen.getByRole('button', {name: 'Retry title generation'}));

        await waitFor(() => expect(aiHubChatsStore.getState().titleGenerationFailures[5]).toBeUndefined());

        expect(generateAiHubChatTitleMock).toHaveBeenCalledWith({chatId: 5, workspaceId: 1});
        expect(screen.queryByRole('button', {name: 'Retry title generation'})).toBeNull();
    });

    it('keeps the retry affordance when the retry fails', async () => {
        generateAiHubChatTitleMock.mockRejectedValue(new Error('still offline'));

        mockChatsDataRef.current = [buildChat({id: 5, title: null})];

        aiHubChatsStore.getState().markTitleGenerationFailed(5, 'model offline');

        renderSidebar();

        fireEvent.click(screen.getByRole('button', {name: 'Retry title generation'}));

        await waitFor(() => expect(aiHubChatsStore.getState().titleGenerationFailures[5]).toBe('still offline'));

        expect(screen.getByRole('button', {name: 'Retry title generation'})).toBeInTheDocument();
    });
});
