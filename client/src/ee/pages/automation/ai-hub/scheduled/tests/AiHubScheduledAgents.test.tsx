import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubScheduledAgents from '@/ee/pages/automation/ai-hub/scheduled/AiHubScheduledAgents';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {agentListMock, useAgentsMock} = vi.hoisted(() => ({agentListMock: vi.fn(), useAgentsMock: vi.fn()}));

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({default: useAgentsMock}));

// The rows are the Agents list's own — covered by AgentListItem's specs, and dependent on the mutations,
// stores and queries that row carries. What belongs to THIS page is which agents reach it.
vi.mock('@/pages/automation/agents/components/agent-list/AgentList', () => ({
    default: (props: {agents: {id: string}[]}) => {
        agentListMock(props.agents);

        return <div data-testid="agent-list" />;
    },
}));

// The create dialog owns mutations and the workspace store; here it stands in as its trigger button.
vi.mock('@/pages/automation/agents/components/AgentDialog', () => ({
    default: ({triggerNode}: {triggerNode?: React.ReactNode}) => <>{triggerNode}</>,
}));

// The hub sidebar carries its own chat queries and stores; its behaviour is covered by its own specs.
vi.mock('@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar', () => ({
    default: () => <div data-testid="ai-hub-chats-sidebar" />,
}));

const scheduledAgent = {
    channels: [{channelType: 'schedule', id: 'schedule-1', parameters: {frequencyKind: 'DAILY', timeOfDay: '09:00'}}],
    id: 'agent-1',
    title: 'Digest',
};

// TooltipProvider: owning a sidebar puts LeftSidebarToggle in the header, whose Radix Tooltip throws outside
// a provider. The app supplies one at the root.
const renderPage = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <TooltipProvider>
                    <AiHubScheduledAgents />
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('AiHubScheduledAgents', () => {
    beforeEach(() => {
        agentListMock.mockReset();
        useAgentsMock.mockReturnValue({agents: [], agentsError: undefined, agentsIsLoading: false});
    });

    it('lists scheduled agents through the Agents list, so a row reads the same on both pages', () => {
        useAgentsMock.mockReturnValue({
            agents: [scheduledAgent],
            agentsError: undefined,
            agentsIsLoading: false,
        });

        renderPage();

        expect(screen.getByTestId('agent-list')).toBeInTheDocument();
        expect(agentListMock).toHaveBeenCalledWith([scheduledAgent]);
    });

    // An agent with no schedule channel is not a scheduled task, however many other channels it carries.
    it('leaves out agents that carry no schedule', () => {
        useAgentsMock.mockReturnValue({
            agents: [
                {channels: [{channelType: 'slack', id: 'slack-1', parameters: {}}], id: 'agent-2', title: 'Chatty'},
            ],
            agentsError: undefined,
            agentsIsLoading: false,
        });

        renderPage();

        expect(agentListMock).not.toHaveBeenCalled();
        expect(screen.getByText('No scheduled tasks')).toBeInTheDocument();
    });

    // A destination inside the AI Hub, so it keeps the hub's sidebar rather than dropping to the bare rail.
    it('keeps the AI Hub sidebar', () => {
        renderPage();

        expect(screen.getByTestId('ai-hub-chats-sidebar')).toBeInTheDocument();
    });

    it('offers the create action from the empty state when nothing is scheduled', () => {
        renderPage();

        expect(screen.getByText('No scheduled tasks')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Task'})).toBeInTheDocument();
    });
});
