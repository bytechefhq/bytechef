import {TooltipProvider} from '@/components/ui/tooltip';
import {AiAgent} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import AgentListItem from './AgentListItem';

const {deleteAgentMutate, navigateMock, publishProjectMutate} = vi.hoisted(() => ({
    deleteAgentMutate: vi.fn(),
    navigateMock: vi.fn(),
    publishProjectMutate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    Link: ({children, to, ...rest}: {children: ReactNode; to: string}) => (
        <a href={to} {...rest}>
            {children}
        </a>
    ),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {project: (id: number) => ['projects', id]},
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    usePublishProjectMutation: () => ({isPending: false, mutate: publishProjectMutate}),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({children, onClick}: {children: ReactNode; onClick?: () => void}) => (
        <button onClick={onClick} type="button">
            {children}
        </button>
    ),
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/automation/agents/hooks/useAiAgentChannelDefinitions', () => ({
    useAiAgentChannelDefinitions: () => ({
        definitionsByType: {chat: {channelType: 'chat', title: 'Chat'}, slack: {channelType: 'slack', title: 'Slack'}},
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: deleteAgentMutate}),
}));

const renderItem = (agent: Partial<AiAgent>) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <AgentListItem
                    agent={
                        {
                            channels: [],
                            description: null,
                            elements: [],
                            id: '1',
                            lastPublishedVersion: 0,
                            projectId: '5',
                            title: 'Refund Agent',
                            ...agent,
                            // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        } as any
                    }
                />
            </TooltipProvider>
        </QueryClientProvider>
    );
};

describe('AgentListItem', () => {
    it('shows when a scheduled agent runs, in words rather than in cron', () => {
        renderItem({
            channels: [
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                {channelType: 'schedule', id: '1', parameters: {frequencyKind: 'DAILY', timeOfDay: '09:30'}} as any,
            ],
        });

        expect(screen.getByText('Daily at 09:30')).toBeInTheDocument();
        expect(screen.queryByText('30 9 * * ?')).not.toBeInTheDocument();
    });

    it('falls back to the expression of a schedule that carries no cadence fields', () => {
        renderItem({
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            channels: [{channelType: 'schedule', id: '1', parameters: {expression: '30 9 * * ?'}} as any],
        });

        expect(screen.getByText('30 9 * * ?')).toBeInTheDocument();
    });

    it('shows when the agent was last modified, as the Projects page row does', () => {
        renderItem({lastModifiedDate: '2026-09-22T07:58:07Z'});

        expect(screen.getByText(/^Modified at /)).toBeInTheDocument();
    });

    it('shows the channels the agent is reached through, leaving out the schedule and the inert workflow call', () => {
        renderItem({
            channels: [
                {channelType: 'chat', id: '1', parameters: {}},
                {channelType: 'slack', id: '2', parameters: {}},
                {channelType: 'schedule', id: '3', parameters: {frequencyKind: 'DAILY', timeOfDay: '09:30'}},
                {channelType: 'workflowCall', id: '4', parameters: {}},
            ],
        } as never);

        expect(screen.getByText('Chat')).toBeInTheDocument();
        expect(screen.getByText('Slack')).toBeInTheDocument();
        expect(screen.queryByText('schedule')).not.toBeInTheDocument();
        expect(screen.queryByText('workflowCall')).not.toBeInTheDocument();
    });

    it('opens the agent from its name, which sits inside the row link rather than a control the row ignores', () => {
        renderItem({});

        const link = screen.getByRole('link', {name: 'Link to agent Refund Agent'});

        expect(link).toHaveAttribute('href', '/automation/projects/5/agents/1');
        expect(link).toContainElement(screen.getByText('Refund Agent'));
    });

    it('shows nothing about schedules for an agent with no schedule channel', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        renderItem({channels: [{channelType: 'chat', id: '1', parameters: {}} as any]});

        expect(screen.queryByText('Scheduled')).not.toBeInTheDocument();
    });

    it('publishes the agent project from the row menu', () => {
        renderItem({projectId: '7'});

        fireEvent.click(screen.getByRole('button', {name: 'Publish Project'}));

        expect(publishProjectMutate).toHaveBeenCalledWith({id: 7, publishProjectRequest: {}});
    });

    it('asks for confirmation before deleting the agent', () => {
        deleteAgentMutate.mockClear();

        renderItem({});

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(deleteAgentMutate).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Agent Deletion'}));

        expect(deleteAgentMutate).toHaveBeenCalledWith({id: '1'});
    });
});
