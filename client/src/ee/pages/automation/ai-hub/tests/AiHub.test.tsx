import {TooltipProvider} from '@/components/ui/tooltip';
import AiHub from '@/ee/pages/automation/ai-hub/AiHub';
import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {act, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const {archivedChatsQueryRef, chatsQueryRef, currentWorkspaceIdRef, switchChatMock} = vi.hoisted(() => ({
    archivedChatsQueryRef: {
        current: {data: [], error: null, isLoading: false} as {
            data: AiHubChatI[] | undefined;
            error: Error | null;
            isLoading: boolean;
        },
    },
    chatsQueryRef: {
        current: {data: [], error: null, isLoading: false} as {
            data: AiHubChatI[] | undefined;
            error: Error | null;
            isLoading: boolean;
        },
    },
    currentWorkspaceIdRef: {current: 1 as number | undefined},
    switchChatMock: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/AiHubHomePanel', () => ({default: () => <div data-testid="home-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubPanel', () => ({default: () => <div data-testid="chat-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubResourcePanel', () => ({
    default: () => <div data-testid="resource-panel" />,
}));
vi.mock('@/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider', () => ({
    AiHubRuntimeProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar', () => ({default: () => <div data-testid="sidebar" />}));
vi.mock('@/ee/pages/automation/ai-hub/hooks/useResetAiHubStoresOnWorkspaceChange', () => ({
    useResetAiHubStoresOnWorkspaceChange: () => {},
}));
vi.mock('@/ee/pages/automation/ai-hub/hooks/useResetAiHubChatOnEnvironmentChange', () => ({
    useResetAiHubChatOnEnvironmentChange: () => {},
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useRecordReferencedArtifacts', () => ({default: () => {}}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat', () => ({
    useSwitchChat: () => switchChatMock,
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: (_workspaceId: number, _environmentId: number, status: 'ACTIVE' | 'ARCHIVED') =>
        status === 'ARCHIVED' ? archivedChatsQueryRef.current : chatsQueryRef.current,
}));
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number | undefined}) => unknown) =>
        selector({currentWorkspaceId: currentWorkspaceIdRef.current}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const WORKFLOW_TAB = {
    id: 'tab-1',
    kind: 'workflow' as const,
    name: 'Lead Scoring',
    projectId: '5',
    projectWorkflowId: 12,
    workflowId: 'wf-1',
};

const buildChat = (overrides: Partial<AiHubChatI> = {}): AiHubChatI => ({
    autoTitled: true,
    createdAt: '2026-10-18T00:00:00Z',
    environmentId: 1,
    id: 7,
    isOwner: true,
    lastPreview: null,
    messageCount: 2,
    ownerUserId: 1,
    status: 'ACTIVE',
    threadId: 'thread-7',
    title: 'Chat',
    updatedAt: '2026-10-18T00:00:00Z',
    userId: 1,
    workspaceId: 1,
    ...overrides,
});

const renderAt = (path: string) =>
    render(
        <MemoryRouter initialEntries={[path]}>
            <TooltipProvider>
                <Routes>
                    <Route element={<AiHub />} path="/automation/ai-hub" />

                    <Route element={<AiHub />} path="/automation/ai-hub/chats/:chatId" />
                </Routes>
            </TooltipProvider>
        </MemoryRouter>
    );

describe('AiHub', () => {
    beforeEach(() => {
        switchChatMock.mockReset();

        currentWorkspaceIdRef.current = 1;

        chatsQueryRef.current = {data: [], error: null, isLoading: false};
        archivedChatsQueryRef.current = {data: [], error: null, isLoading: false};

        aiHubChatsStore.setState({currentChatId: undefined});
        aiHubStore.setState({messages: [], messagesLoading: false});
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            chatsSidebarCollapsed: false,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    describe('home view resource panel', () => {
        afterEach(() => {
            vi.useRealTimers();
        });

        it('renders the resource panel on the home view when a tab is open', () => {
            aiHubTabsStore.setState({activeTabId: 'tab-1', openTabs: [WORKFLOW_TAB], rightPanelOpen: true});

            renderAt('/automation/ai-hub');

            expect(screen.getByTestId('home-panel')).toBeInTheDocument();
            expect(screen.getByTestId('resource-panel')).toBeInTheDocument();
        });

        it('keeps the plain home view when no tabs are open', () => {
            renderAt('/automation/ai-hub');

            expect(screen.getByTestId('home-panel')).toBeInTheDocument();
            expect(screen.queryByTestId('resource-panel')).not.toBeInTheDocument();
        });

        it('keeps the resource panel mounted through the collapse animation after the last tab closes', () => {
            vi.useFakeTimers();

            aiHubTabsStore.setState({activeTabId: 'tab-1', openTabs: [WORKFLOW_TAB], rightPanelOpen: true});

            renderAt('/automation/ai-hub');

            act(() => {
                aiHubTabsStore.setState({activeTabId: undefined, openTabs: []});
            });

            expect(screen.getByTestId('resource-panel')).toBeInTheDocument();

            act(() => {
                vi.advanceTimersByTime(320);
            });

            expect(screen.queryByTestId('resource-panel')).not.toBeInTheDocument();
        });
    });

    describe('chat route', () => {
        it('switches to a deep-linked chat once the chats list resolves', () => {
            const chat = buildChat();

            chatsQueryRef.current = {data: [chat], error: null, isLoading: false};

            renderAt('/automation/ai-hub/chats/7');

            expect(switchChatMock).toHaveBeenCalledWith(chat);
        });

        it('shows the loading state, not the home view, while the chats list loads', () => {
            chatsQueryRef.current = {data: undefined, error: null, isLoading: true};

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByTestId('ai-hub-chat-loading')).toBeInTheDocument();
            expect(screen.queryByTestId('home-panel')).not.toBeInTheDocument();
        });

        it('shows the loading state, not the not-found state, while the workspace id is still unset', () => {
            currentWorkspaceIdRef.current = undefined;

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByTestId('ai-hub-chat-loading')).toBeInTheDocument();
            expect(screen.queryByText('Chat not found')).toBeNull();
        });

        it('shows the not-found state for a chat the API does not return to this user (FORBIDDEN)', () => {
            chatsQueryRef.current = {data: [buildChat({id: 8, threadId: 'thread-8'})], error: null, isLoading: false};

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByText('Chat not found')).toBeInTheDocument();
            expect(screen.getByRole('link', {name: 'Start a new chat'})).toHaveAttribute('href', '/automation/ai-hub');
            expect(screen.queryByTestId('chat-panel')).not.toBeInTheDocument();
            expect(screen.queryByTestId('home-panel')).not.toBeInTheDocument();
            expect(switchChatMock).not.toHaveBeenCalled();
        });

        it('opens a deep-linked archived chat instead of showing not-found', () => {
            const archivedChat = buildChat({status: 'ARCHIVED'});

            archivedChatsQueryRef.current = {data: [archivedChat], error: null, isLoading: false};

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.queryByText('Chat not found')).toBeNull();
            expect(switchChatMock).toHaveBeenCalledWith(archivedChat);
        });

        it('waits for the archived list before deciding a chat is not found', () => {
            archivedChatsQueryRef.current = {data: undefined, error: null, isLoading: true};

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByTestId('ai-hub-chat-loading')).toBeInTheDocument();
            expect(screen.queryByText('Chat not found')).toBeNull();
        });

        it('shows the not-found state when the chats request itself is refused', () => {
            chatsQueryRef.current = {data: undefined, error: new Error('FORBIDDEN'), isLoading: false};
            archivedChatsQueryRef.current = {data: undefined, error: new Error('FORBIDDEN'), isLoading: false};

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByText('Chat not found')).toBeInTheDocument();
            expect(screen.queryByTestId('chat-panel')).not.toBeInTheDocument();
        });

        it('shows the not-found state for a malformed chat id', () => {
            renderAt('/automation/ai-hub/chats/abc');

            expect(screen.getByText('Chat not found')).toBeInTheDocument();
        });

        it('renders the chat panel for the active chat', () => {
            chatsQueryRef.current = {data: [buildChat()], error: null, isLoading: false};

            aiHubChatsStore.setState({currentChatId: 7});

            renderAt('/automation/ai-hub/chats/7');

            expect(screen.getByTestId('chat-panel')).toBeInTheDocument();
            expect(switchChatMock).not.toHaveBeenCalled();
        });

        it('mirrors the active chat into the tabs store', () => {
            chatsQueryRef.current = {data: [buildChat()], error: null, isLoading: false};

            aiHubChatsStore.setState({currentChatId: 7});

            renderAt('/automation/ai-hub/chats/7');

            expect(aiHubTabsStore.getState().activeChatId).toBe(7);
        });
    });
});
