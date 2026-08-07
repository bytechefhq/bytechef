import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubArtifactsCard from '../AiHubArtifactsCard';

const {mockTaskKind, mockUseAiHubTaskArtifactsQuery} = vi.hoisted(() => ({
    mockTaskKind: {current: 'STANDARD'},
    mockUseAiHubTaskArtifactsQuery: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTaskArtifactsQuery: (...args: unknown[]) => mockUseAiHubTaskArtifactsQuery(...args),
    useAiHubTasksQuery: () => ({data: [{id: 7, kind: mockTaskKind.current}]}),
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
        mockTaskKind.current = 'STANDARD';

        mockUseAiHubTaskArtifactsQuery.mockReturnValue({
            data: [{artifactName: 'report.csv', id: 1}],
        });

        aiHubTasksStore.setState({currentTaskId: 7});
        aiHubTabsStore.setState({rightPanelOpen: false});
    });

    it('renders the artifacts of the active task', () => {
        render(<AiHubArtifactsCard />);

        expect(screen.getByText('Artifacts')).toBeInTheDocument();
        expect(screen.getByText('report.csv')).toBeInTheDocument();
    });

    it('renders nothing while the resource panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when the task has no artifacts', () => {
        mockUseAiHubTaskArtifactsQuery.mockReturnValue({data: []});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing for a workflow chat, which never produces artifacts', () => {
        mockTaskKind.current = 'WORKFLOW_CHAT';

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing on the home view, where no task is active', () => {
        aiHubTasksStore.setState({currentTaskId: undefined});

        const {container} = render(<AiHubArtifactsCard />);

        expect(container).toBeEmptyDOMElement();
    });
});
