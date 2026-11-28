import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatArtifactI} from '../../chats/api/chats.api';
import AiHubArtifactsCard from '../AiHubArtifactsCard';

const {mockUseAiHubChatArtifactsQuery} = vi.hoisted(() => ({
    mockUseAiHubChatArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatArtifactsQuery: (...args: unknown[]) => mockUseAiHubChatArtifactsQuery(...args),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiHubChatArtifactMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

const buildArtifact = (overrides: Partial<AiHubChatArtifactI>): AiHubChatArtifactI => ({
    artifactId: 'wf-1',
    artifactName: 'Workflow',
    chatId: 7,
    createdAt: '2026-10-18T00:00:00Z',
    id: 1,
    kind: 'WORKFLOW_CREATED',
    metadataJson: JSON.stringify({projectId: '5', projectWorkflowId: 12}),
    status: 'APPLIED',
    ...overrides,
});

const WORKFLOW_ARTIFACTS: AiHubChatArtifactI[] = [
    buildArtifact({artifactId: 'wf-1', artifactName: 'Created Flow', id: 1, kind: 'WORKFLOW_CREATED'}),
    buildArtifact({
        artifactId: 'wf-2',
        artifactName: 'Updated Flow',
        id: 2,
        kind: 'WORKFLOW_UPDATED',
        metadataJson: JSON.stringify({projectId: '6', projectWorkflowId: 13}),
    }),
    buildArtifact({
        artifactId: 'wf-3',
        artifactName: 'Referenced Flow',
        id: 3,
        kind: 'WORKFLOW_REFERENCED',
        metadataJson: JSON.stringify({projectId: '7', projectWorkflowId: 14}),
    }),
];

const renderCard = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubArtifactsCard />
        </QueryClientProvider>
    );

describe('AiHubArtifactsCard', () => {
    beforeEach(() => {
        mockUseAiHubChatArtifactsQuery.mockReturnValue({data: WORKFLOW_ARTIFACTS});

        aiHubChatsStore.setState({currentChatId: 7});
        useAiHubSettingsStore.setState({artifactsCardCollapsed: false});
        aiHubTabsStore.setState({
            activeChatId: 7,
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    it('lists the WORKFLOW_CREATED, WORKFLOW_UPDATED and WORKFLOW_REFERENCED rows of the active chat', () => {
        renderCard();

        expect(screen.getByText('Artifacts')).toBeInTheDocument();
        expect(screen.getByText('3')).toBeInTheDocument();
        expect(screen.getByText('Created Flow')).toBeInTheDocument();
        expect(screen.getByText('Updated Flow')).toBeInTheDocument();
        expect(screen.getByText('Referenced Flow')).toBeInTheDocument();
    });

    it('opens the workflow tab when a row is clicked', () => {
        renderCard();

        fireEvent.click(screen.getByRole('button', {name: /Updated Flow/}));

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toEqual([
            expect.objectContaining({
                kind: 'workflow',
                name: 'Updated Flow',
                projectId: '6',
                projectWorkflowId: 13,
                workflowId: 'wf-2',
            }),
        ]);
        expect(state.activeTabId).toBe(state.openTabs[0]!.id);
        expect(state.rightPanelOpen).toBe(true);
    });

    it('collapses to its header', () => {
        renderCard();

        fireEvent.click(screen.getByRole('button', {name: /Artifacts/}));

        expect(screen.queryByText('Created Flow')).toBeNull();
        expect(useAiHubSettingsStore.getState().artifactsCardCollapsed).toBe(true);
    });

    it('renders nothing while the resource panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        const {container} = renderCard();

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when the chat has no artifacts', () => {
        mockUseAiHubChatArtifactsQuery.mockReturnValue({data: []});

        const {container} = renderCard();

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing on the home view, where no chat is active', () => {
        aiHubChatsStore.setState({currentChatId: undefined});

        const {container} = renderCard();

        expect(container).toBeEmptyDOMElement();
    });
});
