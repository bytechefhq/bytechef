import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectAgentListItem from './ProjectAgentListItem';

const hoisted = vi.hoisted(() => ({
    mockAgentsLeftSidebarDropdownMenu: vi.fn(() => <button aria-label="agent menu">menu</button>),
}));

vi.mock('@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu', () => ({
    default: hoisted.mockAgentsLeftSidebarDropdownMenu,
}));

vi.mock('@/pages/automation/agents/hooks/useAiAgentChannelDefinitions', () => ({
    useAiAgentChannelDefinitions: () => ({
        definitionsByType: {chat: {channelType: 'chat', title: 'Chat'}, slack: {channelType: 'slack', title: 'Slack'}},
    }),
}));

const scheduledAgent = {
    channels: [
        {
            channelType: 'schedule',
            parameters: {frequencyKind: 'DAILY', timeOfDay: '07:57'},
        },
        {channelType: 'chat', parameters: {}},
    ],
    id: 'agent-1',
    lastModifiedDate: '2024-01-15T10:00:00',
    projectId: '1',
    title: 'Support Bot',
};

const unscheduledAgent = {
    channels: [],
    id: 'agent-2',
    lastModifiedDate: '2024-01-15T10:00:00',
    projectId: '1',
    title: 'Plain Bot',
};

const renderProjectAgentListItem = (agent: typeof scheduledAgent | typeof unscheduledAgent, path = '/start') =>
    render(
        <TooltipProvider>
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route element={<ProjectAgentListItem agent={agent} />} path="/start" />

                    <Route element={<div>Agent Detail Page</div>} path="/automation/projects/:projectId/agents/:id" />
                </Routes>
            </MemoryRouter>
        </TooltipProvider>
    );

describe('ProjectAgentListItem', () => {
    it('renders the agent title', () => {
        renderProjectAgentListItem(scheduledAgent);

        expect(screen.getByText('Support Bot')).toBeInTheDocument();
    });

    it('renders the schedule summary for a scheduled agent', () => {
        renderProjectAgentListItem(scheduledAgent);

        expect(screen.getByText('Daily at 07:57')).toBeInTheDocument();
    });

    it('puts the schedule beside the last modified date rather than inside the title link', () => {
        renderProjectAgentListItem(scheduledAgent);

        const schedule = screen.getByText('Daily at 07:57');

        expect(screen.getByTestId('agent-1-link')).not.toContainElement(schedule);
        expect(schedule.closest('div')?.parentElement).toContainElement(screen.getAllByText(/Modified at/)[0]);
    });

    it('shows the agent channels as chips, leaving the schedule to its own column', () => {
        renderProjectAgentListItem(scheduledAgent);

        expect(screen.getByText('Chat')).toBeInTheDocument();
        expect(screen.queryByText('schedule')).not.toBeInTheDocument();
    });

    it('renders no schedule summary for an unscheduled agent', () => {
        renderProjectAgentListItem(unscheduledAgent);

        expect(screen.queryByText(/Daily at|Weekly on|Monthly on|Every|Hourly/)).not.toBeInTheDocument();
    });

    it('renders the last modified date', () => {
        renderProjectAgentListItem(scheduledAgent);

        expect(screen.getAllByText(/Modified at/).length).toBeGreaterThan(0);
    });

    it('renders the always-visible menu trigger', () => {
        renderProjectAgentListItem(scheduledAgent);

        expect(screen.getByRole('button', {name: 'agent menu'})).toBeInTheDocument();
        expect(hoisted.mockAgentsLeftSidebarDropdownMenu).toHaveBeenCalledWith(
            expect.objectContaining({agent: scheduledAgent, alwaysVisibleTrigger: true, current: false}),
            undefined
        );
    });

    it('navigates to the agent path when the row is clicked', () => {
        renderProjectAgentListItem(scheduledAgent);

        fireEvent.click(screen.getByText('Support Bot'));

        expect(screen.getByText('Agent Detail Page')).toBeInTheDocument();
    });
});
