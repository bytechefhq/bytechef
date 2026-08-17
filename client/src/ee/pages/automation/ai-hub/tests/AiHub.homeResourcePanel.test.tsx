import AiHub from '@/ee/pages/automation/ai-hub/AiHub';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {act, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/AiHubHomePanel', () => ({default: () => <div data-testid="home-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubPanel', () => ({default: () => <div data-testid="chat-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubResourcePanel', () => ({
    default: () => <div data-testid="resource-panel" />,
}));
vi.mock('@/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider', () => ({
    AiHubRuntimeProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubChatsSidebarPinButton', () => ({
    default: () => <div data-testid="sidebar-pin-button" />,
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar', () => ({default: () => <div data-testid="sidebar" />}));
vi.mock('@/ee/pages/automation/ai-hub/hooks/useResetAiHubStoresOnWorkspaceChange', () => ({
    useResetAiHubStoresOnWorkspaceChange: () => {},
}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useRecordReferencedArtifacts', () => ({default: () => {}}));
vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: () => ({data: [], isLoading: false}),
}));
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

describe('AiHub home view resource panel', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    it('renders the resource panel on the home view when a tab is open', () => {
        aiHubTabsStore.setState({
            activeTabId: 'tab-1',
            openTabs: [{fileId: '1', id: 'tab-1', kind: 'file', name: 'a.txt', viewMode: 'editor'}],
            rightPanelOpen: true,
        });

        render(
            <MemoryRouter initialEntries={['/automation/ai-hub']}>
                <AiHub />
            </MemoryRouter>
        );

        expect(screen.getByTestId('home-panel')).toBeInTheDocument();
        expect(screen.getByTestId('resource-panel')).toBeInTheDocument();
    });

    it('keeps the plain home view when no tabs are open', () => {
        render(
            <MemoryRouter initialEntries={['/automation/ai-hub']}>
                <AiHub />
            </MemoryRouter>
        );

        expect(screen.getByTestId('home-panel')).toBeInTheDocument();
        expect(screen.queryByTestId('resource-panel')).not.toBeInTheDocument();
    });

    describe('closing the last open tab on the home view', () => {
        afterEach(() => {
            vi.useRealTimers();
        });

        it('keeps the resource panel mounted through the collapse animation, then unmounts it once the animation window elapses', () => {
            vi.useFakeTimers();

            aiHubTabsStore.setState({
                activeTabId: 'tab-1',
                openTabs: [{fileId: '1', id: 'tab-1', kind: 'file', name: 'a.txt', viewMode: 'editor'}],
                rightPanelOpen: true,
            });

            render(
                <MemoryRouter initialEntries={['/automation/ai-hub']}>
                    <AiHub />
                </MemoryRouter>
            );

            expect(screen.getByTestId('resource-panel')).toBeInTheDocument();

            // Mirrors the real closeTab implementation: it empties openTabs but does not touch
            // rightPanelOpen. hasActiveChat stays false (home view), so hasResourceContent flips false
            // in this same update.
            act(() => {
                aiHubTabsStore.setState({activeTabId: undefined, openTabs: []});
            });

            // Must still be mounted right after the last tab closes — an immediate unmount here is
            // exactly the reviewer-flagged flash: the pane is still ~62% wide (the width-collapse
            // animation hasn't run yet) but would render empty.
            expect(screen.getByTestId('resource-panel')).toBeInTheDocument();

            act(() => {
                vi.advanceTimersByTime(320);
            });

            expect(screen.queryByTestId('resource-panel')).not.toBeInTheDocument();
        });
    });
});
