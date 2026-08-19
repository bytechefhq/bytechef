import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubArtifactsCard from '../AiHubArtifactsCard';

const {mockChatKind, mockUseAiHubChatArtifactsQuery} = vi.hoisted(() => ({
    mockChatKind: {current: 'STANDARD'},
    mockUseAiHubChatArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatArtifactsQuery: (...args: unknown[]) => mockUseAiHubChatArtifactsQuery(...args),
    useAiHubChatsQuery: () => ({data: [{id: 7, kind: mockChatKind.current}]}),
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow', () => ({
    default: ({artifact}: {artifact: {artifactName: string}}) => <div>{artifact.artifactName}</div>,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

describe('AiHubArtifactsCard', () => {
    beforeEach(() => {
        mockChatKind.current = 'STANDARD';

        mockUseAiHubChatArtifactsQuery.mockReturnValue({
            data: [{artifactName: 'report.csv', id: 1}],
        });

        aiHubChatsStore.setState({currentChatId: 7});
        aiHubTabsStore.setState({rightPanelOpen: false});
    });

    it('renders the artifacts of the active chat', () => {
        render(<AiHubArtifactsCard />);

        expect(screen.getByText('Artifacts')).toBeInTheDocument();
        expect(screen.getByText('report.csv')).toBeInTheDocument();
    });

    it('renders nothing while the resource panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when the chat has no artifacts', () => {
        mockUseAiHubChatArtifactsQuery.mockReturnValue({data: []});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing for a workflow chat, which never produces artifacts', () => {
        mockChatKind.current = 'WORKFLOW_CHAT';

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing on the home view, where no chat is active', () => {
        aiHubChatsStore.setState({currentChatId: undefined});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });
});
