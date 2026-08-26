import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AgentDetailHeader from './AgentDetailHeader';

// The header's dialogs and popovers drag in the whole project-deployment / publish surface, which these
// tests say nothing about: they are about the version pill alone, so every child that owns a network call
// or a portal is stubbed to nothing.
vi.mock('@/pages/automation/agents/components/AgentDialog', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentVisibilityDialog', () => ({default: () => null}));
vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => null,
}));
vi.mock('@/pages/automation/project/components/ProjectVersionHistorySheet', () => ({default: () => null}));
vi.mock('@/pages/automation/project/components/project-header/components/PublishPopover', () => ({
    default: () => null,
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({useIsVisibilityEditionEnabled: () => false}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentVersionsQuery: () => ({data: undefined}),
    useDeleteAiAgentMutation: () => ({isPending: false, mutate: vi.fn()}),
    usePublishAiAgentMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>{ui}</MemoryRouter>
        </QueryClientProvider>
    );
};

const renderHeader = (lastPublishedVersion: number) =>
    wrap(
        <AgentDetailHeader
            id="agent-1"
            lastPublishedVersion={lastPublishedVersion}
            onToggleTestPanel={vi.fn()}
            projectId="42"
            testPanelOpen={false}
            title="Agent1"
        />
    );

describe('AgentDetailHeader', () => {
    // The pill names the version the editor writes into, which is the draft and only ever the draft — the
    // project header's own rule. A never-published agent's draft is the V1 that publishing would mint.
    it('reads V1 DRAFT before the agent has ever been published', () => {
        renderHeader(0);

        expect(screen.getByText('V1')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    // The regression this file exists for. The pill used to be derived from lastPublishedVersion alone and
    // sat at "V2 PUBLISHED" through every later edit; naming the draft only when it had diverged then put
    // PUBLISHED back after each publish, which the project header never does.
    it('names the next version as the draft once the agent has been published', () => {
        renderHeader(3);

        expect(screen.getByText('V4')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
        expect(screen.queryByText('PUBLISHED')).not.toBeInTheDocument();
    });
});
