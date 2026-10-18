import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatArtifactI} from '../../chats/api/chats.api';
import AiHubArtifactRow from '../AiHubArtifactRow';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiHubChatArtifactMutation: () => ({isPending: false, mutate: mutateMock}),
}));

function buildArtifact(overrides: Partial<AiHubChatArtifactI> = {}): AiHubChatArtifactI {
    return {
        artifactId: 'wf-1',
        artifactName: 'Lead Scoring',
        chatId: 1,
        createdAt: new Date().toISOString(),
        id: 3,
        kind: 'WORKFLOW_CREATED',
        metadataJson: JSON.stringify({projectId: '5', projectWorkflowId: 12}),
        status: 'APPLIED',
        ...overrides,
    };
}

function renderRow(ui: ReactElement) {
    return render(<QueryClientProvider client={new QueryClient()}>{ui}</QueryClientProvider>);
}

describe('AiHubArtifactRow', () => {
    beforeEach(() => {
        mutateMock.mockReset();

        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            chatsSidebarCollapsed: false,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    it('opens the workflow in the resource panel on click', () => {
        renderRow(<AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />);

        fireEvent.click(screen.getByRole('button', {name: /Lead Scoring/}));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]!.kind).toBe('workflow');
        expect(state.rightPanelOpen).toBe(true);
    });

    it('shows a remove affordance only for a referenced workflow', () => {
        const queryClient = new QueryClient();

        const {rerender} = render(
            <QueryClientProvider client={queryClient}>
                <AiHubArtifactRow artifact={buildArtifact()} workspaceId={1} />
            </QueryClientProvider>
        );

        expect(screen.queryByRole('button', {name: 'Remove Lead Scoring'})).toBeNull();

        rerender(
            <QueryClientProvider client={queryClient}>
                <AiHubArtifactRow artifact={buildArtifact({kind: 'WORKFLOW_REFERENCED'})} workspaceId={1} />
            </QueryClientProvider>
        );

        expect(screen.getByRole('button', {name: 'Remove Lead Scoring'})).toBeInTheDocument();
    });

    it('removes a referenced workflow without opening it', () => {
        renderRow(<AiHubArtifactRow artifact={buildArtifact({kind: 'WORKFLOW_REFERENCED'})} workspaceId={1} />);

        fireEvent.click(screen.getByRole('button', {name: 'Remove Lead Scoring'}));

        expect(mutateMock).toHaveBeenCalledWith({input: {artifactId: '3', workspaceId: '1'}});
        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('renders a non-clickable artifact as static text', () => {
        renderRow(<AiHubArtifactRow artifact={buildArtifact({metadataJson: null})} workspaceId={1} />);

        expect(screen.queryByRole('button')).toBeNull();
        expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
    });
});
