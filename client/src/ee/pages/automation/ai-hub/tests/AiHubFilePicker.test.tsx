import AiHubFilePicker from '@/ee/pages/automation/ai-hub/AiHubFilePicker';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockUseAiHubChatArtifactsQuery} = vi.hoisted(() => ({
    mockUseAiHubChatArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatArtifactsQuery: (...args: unknown[]) => mockUseAiHubChatArtifactsQuery(...args),
}));

// The picker's other branches fan out into project/workflow/file/data-table/knowledge-base queries that
// are irrelevant to the artifacts branch; stub them flat so the test exercises one branch only.
vi.mock('@/shared/middleware/graphql', () => ({
    useDataTablesQuery: () => ({data: undefined}),
    useGetAssetFilesQuery: () => ({data: undefined}),
    useKnowledgeBasesQuery: () => ({data: undefined}),
    useWorkspaceProjectWorkflowsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useInfiniteWorkspaceProjectWorkflowExecutionsQuery: () => ({
        data: undefined,
        fetchNextPage: vi.fn(),
        hasNextPage: false,
        isFetchingNextPage: false,
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

describe('AiHubFilePicker artifacts branch', () => {
    beforeEach(() => {
        aiHubChatsStore.setState({currentChatId: 7});

        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            chatsSidebarCollapsed: true,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });

        mockUseAiHubChatArtifactsQuery.mockReturnValue({
            data: [
                {
                    artifactId: 'file-1',
                    artifactName: 'report.csv',
                    chatId: 7,
                    createdAt: new Date().toISOString(),
                    id: 1,
                    kind: 'FILE_CREATED',
                    metadataJson: null,
                    status: 'APPLIED',
                },
            ],
        });
    });

    it('drills into Artifacts and opens the picked artifact as a tab', () => {
        wrap(<AiHubFilePicker />);

        fireEvent.click(screen.getByRole('button', {name: 'Add resource'}));
        fireEvent.click(screen.getByText('Artifacts'));
        fireEvent.click(screen.getByText('report.csv'));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.kind).toBe('file');
    });

    it('shows an empty state when the chat has no artifacts', () => {
        mockUseAiHubChatArtifactsQuery.mockReturnValue({data: []});

        wrap(<AiHubFilePicker />);

        fireEvent.click(screen.getByRole('button', {name: 'Add resource'}));
        fireEvent.click(screen.getByText('Artifacts'));

        expect(screen.getByText('No artifacts yet.')).toBeInTheDocument();
    });
});
