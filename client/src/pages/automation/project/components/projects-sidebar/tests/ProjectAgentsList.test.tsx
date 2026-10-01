import {TooltipProvider} from '@/components/ui/tooltip';
import {
    AI_AGENT_CHANNEL_DEFINITIONS,
    aiAgentChannelDefinitionsQueryResult,
} from '@/pages/automation/agents/hooks/aiAgentChannelDefinitions.fixture';
import {render, screen} from '@testing-library/react';
import {ComponentProps} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectAgentsList from '../components/ProjectAgentsList';

vi.mock('@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu', () => ({
    default: ({agent, current}: {agent: {title: string}; current: boolean}) => (
        <button aria-label={`${agent.title} menu`} data-current={current} />
    ),
}));

const mockAgents = [
    {
        channels: [
            {channelType: 'workflowCall', id: 'channel-workflow-call'},
            {channelType: 'slack', id: 'channel-slack'},
            {channelType: 'telegram', id: 'channel-telegram'},
        ],
        id: '1',
        lastModifiedDate: '2024-01-15T10:00:00',
        projectId: '7',
        title: 'Support Bot',
    },
    {channels: [], id: '2', lastModifiedDate: '2024-01-16T10:00:00', projectId: '8', title: 'Other Project Bot'},
];

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({
        agents: mockAgents,
        agentsIsLoading: false,
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentChannelDefinitionsQuery: () => aiAgentChannelDefinitionsQueryResult(AI_AGENT_CHANNEL_DEFINITIONS),
}));

const calculateTimeDifference = vi.fn().mockReturnValue('3 days ago');

const renderList = (props: Partial<ComponentProps<typeof ProjectAgentsList>> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ProjectAgentsList
                    calculateTimeDifference={calculateTimeDifference}
                    emptyMessage="No agents yet."
                    projectId={0}
                    {...props}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('ProjectAgentsList', () => {
    it('lists only the agents of the given project, linking to the project-scoped route', () => {
        renderList({currentAgentId: '1', projectId: 7});

        expect(screen.getByRole('link', {name: /Support Bot/})).toHaveAttribute(
            'href',
            '/automation/projects/7/agents/1'
        );
        expect(screen.queryByText('Other Project Bot')).not.toBeInTheDocument();
    });

    it('gives each agent row an Edit/Delete menu that knows whether it is the open agent', () => {
        renderList({currentAgentId: '1', projectId: 0});

        expect(screen.getByRole('button', {name: 'Support Bot menu'})).toHaveAttribute('data-current', 'true');
        expect(screen.getByRole('button', {name: 'Other Project Bot menu'})).toHaveAttribute('data-current', 'false');
    });

    it('shows the empty message instead of a list when the project has no agents', () => {
        renderList({emptyMessage: 'No agents in this project.', projectId: 99});

        expect(screen.getByText('No agents in this project.')).toBeInTheDocument();
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
    });

    it('shows agents of every project when projectId is 0 (all projects)', () => {
        renderList({projectId: 0});

        expect(screen.getByText('Support Bot')).toBeInTheDocument();
        expect(screen.getByText('Other Project Bot')).toBeInTheDocument();
    });

    it('shows the first non-hidden channel as a badge and "+N" for the rest', () => {
        renderList({projectId: 7});

        expect(screen.getByText('Slack')).toBeInTheDocument();
        expect(screen.getByText('+1')).toBeInTheDocument();
        expect(screen.queryByText('Workflow Call')).not.toBeInTheDocument();
    });

    it('renders no channel badge for an agent with no channels', () => {
        renderList({projectId: 8});

        expect(screen.queryByText('+1')).not.toBeInTheDocument();
    });

    it("renders the Edited date from the agent's lastModifiedDate using the shared date formatting", () => {
        renderList({projectId: 7});

        expect(calculateTimeDifference).toHaveBeenCalledWith('2024-01-15T10:00:00');
        expect(screen.getByText('Edited')).toBeInTheDocument();
        expect(screen.getByText('3 days ago')).toBeInTheDocument();
    });
});
