import AiHubFilePicker from '@/ee/pages/automation/ai-hub/AiHubFilePicker';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockUseAiHubTaskArtifactsQuery} = vi.hoisted(() => ({
    mockUseAiHubTaskArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTaskArtifactsQuery: (...args: unknown[]) => mockUseAiHubTaskArtifactsQuery(...args),
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
        aiHubTasksStore.setState({currentTaskId: 7});

        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });

        mockUseAiHubTaskArtifactsQuery.mockReturnValue({
            data: [
                {
                    artifactId: 'file-1',
                    artifactName: 'report.csv',
                    createdAt: new Date().toISOString(),
                    id: 1,
                    kind: 'FILE_CREATED',
                    metadataJson: null,
                    status: 'APPLIED',
                    taskId: 7,
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

    it('shows an empty state when the task has no artifacts', () => {
        mockUseAiHubTaskArtifactsQuery.mockReturnValue({data: []});

        wrap(<AiHubFilePicker />);

        fireEvent.click(screen.getByRole('button', {name: 'Add resource'}));
        fireEvent.click(screen.getByText('Artifacts'));

        expect(screen.getByText('No artifacts yet.')).toBeInTheDocument();
    });
});
