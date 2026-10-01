import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectAgentList from './ProjectAgentList';

vi.mock('@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({
        agents: [
            {id: 'a1', projectId: '1', title: 'Support Bot'},
            {id: 'a2', projectId: '5', title: 'Other Project Bot'},
        ],
    }),
}));

vi.mock('@/pages/automation/projects/components/project-agent-list/ProjectAgentCreationActions', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({placement}: any) => <button aria-label="Create Agent">Create Agent ({placement})</button>,
}));

const renderWithProviders = (ui: ReactElement) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <MemoryRouter>{ui}</MemoryRouter>
            </TooltipProvider>
        </QueryClientProvider>
    );
};

const projectWithAgent = {id: 1, workspaceId: 10} as Project;
const projectWithoutAgents = {id: 2, workspaceId: 10} as Project;

describe('ProjectAgentList', () => {
    it('lists the agents without its own create button when the project has agents (the tab row has one)', () => {
        renderWithProviders(<ProjectAgentList project={projectWithAgent} />);

        expect(screen.getByText('Support Bot')).toBeInTheDocument();
        expect(screen.queryByText('Other Project Bot')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Agent'})).not.toBeInTheDocument();
    });

    it("links each agent row to the agent's detail page", () => {
        renderWithProviders(<ProjectAgentList project={projectWithAgent} />);

        expect(screen.getByText('Support Bot').closest('a')).toHaveAttribute(
            'href',
            '/automation/projects/1/agents/a1'
        );
    });

    it('shows the create-agent action inside the empty state when the project has no agents', () => {
        renderWithProviders(<ProjectAgentList project={projectWithoutAgents} />);

        expect(screen.getByText('No Agents')).toBeInTheDocument();
        expect(screen.getByText('Get started by creating a new agent.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Agent'})).toHaveTextContent('emptyState');
    });
});
