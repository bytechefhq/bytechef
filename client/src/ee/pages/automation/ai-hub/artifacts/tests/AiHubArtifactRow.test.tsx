import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubTaskArtifactI} from '../../tasks/api/tasks.api';
import AiHubArtifactRow from '../AiHubArtifactRow';

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiHubTaskArtifactMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

function buildArtifact(overrides: Partial<AiHubTaskArtifactI> = {}): AiHubTaskArtifactI {
    return {
        artifactId: 'file-1',
        artifactName: 'report.csv',
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'FILE_CREATED',
        metadataJson: null,
        status: 'APPLIED',
        taskId: 1,
        ...overrides,
    };
}

// AiHubArtifactRow calls useQueryClient() directly (for the delete mutation's onSuccess invalidation),
// so it needs a real QueryClientProvider ancestor even though useDeleteAiHubTaskArtifactMutation itself
// is mocked above.
function renderRow(ui: ReactElement) {
    return render(<QueryClientProvider client={new QueryClient()}>{ui}</QueryClientProvider>);
}

describe('AiHubArtifactRow', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });
    });

    it('opens the artifact in the resource panel on click', () => {
        renderRow(<AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />);

        fireEvent.click(screen.getByRole('button', {name: /report.csv/}));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.kind).toBe('file');
        expect(state.rightPanelOpen).toBe(true);
    });

    it('shows a remove affordance only for user-attached reference kinds', () => {
        const queryClient = new QueryClient();

        const {rerender} = render(
            <QueryClientProvider client={queryClient}>
                <AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />
            </QueryClientProvider>
        );

        expect(screen.queryByRole('button', {name: 'Remove report.csv'})).toBeNull();

        rerender(
            <QueryClientProvider client={queryClient}>
                <AiHubArtifactRow artifact={buildArtifact({kind: 'FILE_REFERENCED'})} workspaceId={1} />
            </QueryClientProvider>
        );

        expect(screen.getByRole('button', {name: 'Remove report.csv'})).toBeInTheDocument();
    });

    it('renders a non-clickable artifact as static text', () => {
        // WORKFLOW_CREATED without a projectId in metadata has nowhere to route to.
        renderRow(<AiHubArtifactRow artifact={buildArtifact({kind: 'WORKFLOW_CREATED'})} workspaceId={1} />);

        expect(screen.queryByRole('button')).toBeNull();
        expect(screen.getByText('report.csv')).toBeInTheDocument();
    });
});
