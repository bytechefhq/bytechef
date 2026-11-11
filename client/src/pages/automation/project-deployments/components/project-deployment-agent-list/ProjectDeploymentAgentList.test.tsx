import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentAgentList from './ProjectDeploymentAgentList';
import {ProjectDeploymentAgentType} from './ProjectDeploymentAgentListItem';

const hoisted = vi.hoisted(() => ({
    agents: [] as {id: string; projectId: string}[],
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueriesMock}),
    };
});

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({agents: hoisted.agents}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: ({onSuccess}: {onSuccess: () => void}) => ({
        isPending: false,
        mutate: (request: unknown) => {
            hoisted.mutateMock(request);

            onSuccess();
        },
    }),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/agent-deployment-channel-list/AgentDeploymentChannelList',
    () => ({
        AgentDeploymentSchedule: () => null,
        default: ({title, workflows}: {title: string; workflows: {workflowId: string}[]}) => (
            <div data-testid="channel-list">
                {title} channels for {workflows.map((workflow) => workflow.workflowId).join(',')}
            </div>
        ),
    })
);

const agentDeployment = {
    agentId: '1',
    agentTitle: 'Support Bot',
    id: '50',
    workflows: [{enabled: true, triggers: [], workflowId: 'agent-workflow'}],
} as unknown as ProjectDeploymentAgentType;

const projectDeploymentWorkflows = [
    {enabled: true, workflowId: 'workflow1'},
    {
        enabled: true,
        lastExecutionDate: new Date(2026, 0, 2, 3, 4, 5),
        lastExecutionStatus: 'COMPLETED',
        workflowId: 'agent-workflow',
    },
] as ProjectDeploymentWorkflow[];

const renderList = (agentDeployments: ProjectDeploymentAgentType[] = [agentDeployment]) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ProjectDeploymentAgentList
                    agentDeployments={agentDeployments}
                    projectDeploymentWorkflows={projectDeploymentWorkflows}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('ProjectDeploymentAgentList', () => {
    beforeEach(() => {
        hoisted.agents = [{id: '1', projectId: '7'}];
        hoisted.invalidateQueriesMock.mockReset();
        hoisted.mutateMock.mockReset();
    });

    it("lists each agent with its channels and its generated workflow's last execution", () => {
        renderList();

        expect(screen.getByText('Support Bot')).toBeInTheDocument();
        expect(screen.getByTestId('channel-list')).toHaveTextContent('Support Bot channels for agent-workflow');
        expect(screen.getByText('completed')).toBeInTheDocument();
    });

    it('links the agent title to the agent page when the agent is resolvable', () => {
        renderList();

        expect(screen.getByRole('link', {name: 'Link to agent Support Bot'})).toHaveAttribute(
            'href',
            '/automation/projects/7/agents/1'
        );
    });

    it('shows the title without a link when the agent is not resolvable', () => {
        hoisted.agents = [];

        renderList();

        expect(screen.getByText('Support Bot')).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it("toggles the agent's generated workflow in this deployment from the enable switch", async () => {
        const user = userEvent.setup();

        renderList();

        const enableSwitch = screen.getByRole('switch', {name: 'Enable agent Support Bot'});

        expect(enableSwitch).toBeChecked();

        await user.click(enableSwitch);

        expect(hoisted.mutateMock).toHaveBeenCalledWith({enable: false, id: 50, workflowId: 'agent-workflow'});
        expect(hoisted.invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['projectDeployments']});
        expect(hoisted.invalidateQueriesMock).toHaveBeenCalledWith({queryKey: ['aiAgentDeployments']});
    });

    it('shows No Agents for a deployment without agents', () => {
        renderList([]);

        expect(screen.getByText('No Agents')).toBeInTheDocument();
    });
});
