import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectWorkflowList from '../ConnectedUserProjectWorkflowList';
import {
    ConnectedUserProjectWorkflowType,
    ConnectedUserWorkflowTemplateSourceI,
    ConnectedUserWorkflowTemplateType,
} from '../ConnectedUserProjectWorkflowListItem';

vi.mock('../ConnectedUserProjectWorkflowListItem', () => ({
    default: ({
        connectedUserProjectWorkflow,
        projectName,
        workflowTemplate,
    }: {
        connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
        projectName?: string;
        workflowTemplate?: ConnectedUserWorkflowTemplateType;
    }) => (
        <li data-project-name={projectName ?? ''} data-template-uuid={workflowTemplate?.workflowUuid ?? ''}>
            {connectedUserProjectWorkflow.workflow.label}
        </li>
    ),
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <>{children}</>,
    TooltipContent: () => null,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <span>{children}</span>,
}));

const createWorkflowTemplate = (workflowUuid: string): ConnectedUserWorkflowTemplateType => ({
    components: [],
    description: null,
    label: workflowUuid,
    lastModifiedDate: null,
    permissionExpression: null,
    triggers: [],
    workflowId: `workflow-${workflowUuid}`,
    workflowUuid,
});

const createConnectedUserProjectWorkflow = (
    id: string,
    label: string,
    sourceWorkflowUuids: {automationWorkflowUuid?: string; copiedFromWorkflowUuid?: string} = {}
): ConnectedUserProjectWorkflowType => ({
    automationWorkflowUuid: sourceWorkflowUuids.automationWorkflowUuid ?? null,
    connectedUserId: '1050',
    copiedFromWorkflowUuid: sourceWorkflowUuids.copiedFromWorkflowUuid ?? null,
    enabled: true,
    id,
    lastExecutionDate: null,
    projectId: '0',
    staticWebhookUrl: null,
    workflow: {id: `workflow-${id}`, label, triggers: []},
    workflowUuid: `uuid-${id}`,
    workflowVersion: 1,
});

const workflowTemplateSourcesByWorkflowUuid = new Map<string, ConnectedUserWorkflowTemplateSourceI>([
    ['template-standup', {projectName: 'Team Rituals', workflowTemplate: createWorkflowTemplate('template-standup')}],
    [
        'template-greeting',
        {projectName: 'Automation Playground', workflowTemplate: createWorkflowTemplate('template-greeting')},
    ],
]);

const expandGroups = async () => {
    for (const groupName of ['Automation Playground', 'Team Rituals', 'Other']) {
        const group = screen.queryByRole('region', {name: groupName});

        if (group) {
            await userEvent.click(within(group).getByRole('button', {name: new RegExp(groupName)}));
        }
    }
};

describe('ConnectedUserProjectWorkflowList', () => {
    it('groups workflows under their source project, sorted by name, with unmatched workflows last', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[
                    createConnectedUserProjectWorkflow('1', 'Custom Workflow'),
                    createConnectedUserProjectWorkflow('2', 'Daily standup reminder', {
                        copiedFromWorkflowUuid: 'template-standup',
                    }),
                    createConnectedUserProjectWorkflow('3', 'Greet on Request', {
                        automationWorkflowUuid: 'template-greeting',
                    }),
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const groups = screen.getAllByRole('region');

        expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual([
            'Automation Playground',
            'Team Rituals',
            'Other',
        ]);

        expect(screen.queryByText('Greet on Request')).not.toBeInTheDocument();

        await expandGroups();

        expect(within(groups[0]).getByText('Greet on Request')).toHaveAttribute(
            'data-project-name',
            'Automation Playground'
        );
        expect(within(groups[0]).getByText('Greet on Request')).toHaveAttribute(
            'data-template-uuid',
            'template-greeting'
        );
        expect(within(groups[1]).getByText('Daily standup reminder')).toHaveAttribute(
            'data-project-name',
            'Team Rituals'
        );
        expect(within(groups[2]).getByText('Custom Workflow')).toHaveAttribute('data-project-name', '');
    });

    it('shows each group as a collapsed card with its workflow count and latest execution', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[
                    {
                        ...createConnectedUserProjectWorkflow('1', 'Greet on Request', {
                            automationWorkflowUuid: 'template-greeting',
                        }),
                        lastExecutionDate: '2026-10-28T10:00:00Z',
                    },
                    {
                        ...createConnectedUserProjectWorkflow('2', 'Summarize New Email', {
                            copiedFromWorkflowUuid: 'template-greeting',
                        }),
                        lastExecutionDate: '2026-10-28T12:00:00Z',
                    },
                    createConnectedUserProjectWorkflow('3', 'Custom Workflow'),
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});
        const latestExecutionDate = new Date('2026-10-28T12:00:00Z');

        expect(within(playgroundGroup).getByText('2 workflows')).toBeInTheDocument();
        expect(
            within(playgroundGroup).getByText(
                `Executed at ${latestExecutionDate.toLocaleDateString()} ${latestExecutionDate.toLocaleTimeString()}`
            )
        ).toBeInTheDocument();
        expect(within(playgroundGroup).queryByRole('heading', {name: 'Workflows'})).not.toBeInTheDocument();

        await userEvent.click(within(playgroundGroup).getByRole('button', {name: /Automation Playground/}));

        expect(within(playgroundGroup).getByRole('heading', {name: 'Workflows'})).toBeInTheDocument();
        expect(within(playgroundGroup).getByText('Greet on Request')).toBeInTheDocument();

        const otherGroup = screen.getByRole('region', {name: 'Other'});

        expect(within(otherGroup).getByText('1 workflow')).toBeInTheDocument();
        expect(within(otherGroup).getByText('No executions')).toBeInTheDocument();
    });
});
