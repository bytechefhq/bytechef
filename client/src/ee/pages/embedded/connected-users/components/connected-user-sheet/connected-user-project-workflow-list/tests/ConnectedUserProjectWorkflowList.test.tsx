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
        configurationProjectVersion,
        connectedUserProjectWorkflow,
        projectName,
        workflowTemplate,
    }: {
        configurationProjectVersion?: number;
        connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
        projectName?: string;
        workflowTemplate?: ConnectedUserWorkflowTemplateType;
    }) => (
        <li
            data-configuration-project-version={configurationProjectVersion ?? ''}
            data-project-name={projectName ?? ''}
            data-template-uuid={workflowTemplate?.workflowUuid ?? ''}
        >
            {connectedUserProjectWorkflow.workflow.label}
        </li>
    ),
}));

vi.mock('../ConnectedUserProjectConfigurationMenu', () => ({
    default: ({
        currentConfiguration,
        projectId,
    }: {
        currentConfiguration: {configurationName: string};
        projectId: string;
    }) => (
        <button type="button">{`Change configuration ${currentConfiguration.configurationName} ${projectId}`}</button>
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
    sourceWorkflowUuids: {automationUuid?: string; copiedFromWorkflowUuid?: string} = {}
): ConnectedUserProjectWorkflowType => ({
    automationUuid: sourceWorkflowUuids.automationUuid ?? null,
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
    [
        'template-standup',
        {projectId: '20', projectName: 'Team Rituals', workflowTemplate: createWorkflowTemplate('template-standup')},
    ],
    [
        'template-greeting',
        {
            projectId: '10',
            projectName: 'Automation Playground',
            workflowTemplate: createWorkflowTemplate('template-greeting'),
        },
    ],
]);

describe('ConnectedUserProjectWorkflowList', () => {
    it('groups workflows under their source project, sorted by name, with workflows created by the user last', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[
                    createConnectedUserProjectWorkflow('1', 'Custom Workflow'),
                    {
                        ...createConnectedUserProjectWorkflow('2', 'Daily standup reminder', {
                            copiedFromWorkflowUuid: 'template-standup',
                        }),
                        projectId: '20',
                    },
                    {
                        ...createConnectedUserProjectWorkflow('3', 'Greet on Request', {
                            automationUuid: 'template-greeting',
                        }),
                        projectId: '10',
                    },
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const groups = screen.getAllByRole('region');

        expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual([
            'Automation Playground',
            'Team Rituals',
            'Created by this user',
        ]);

        for (const group of groups) {
            await userEvent.click(within(group).getByRole('button', {name: 'Toggle workflows'}));
        }

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

    it('shows a project group as a folder header above a collapsed card of its workflows', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[
                    createConnectedUserProjectWorkflow('1', 'Greet on Request', {
                        automationUuid: 'template-greeting',
                    }),
                    createConnectedUserProjectWorkflow('2', 'Summarize New Email', {
                        copiedFromWorkflowUuid: 'template-greeting',
                    }),
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});

        expect(within(playgroundGroup).getByRole('heading', {name: 'Automation Playground'})).toBeInTheDocument();
        expect(playgroundGroup.querySelector('svg.lucide-folder')).toBeInTheDocument();
        expect(within(playgroundGroup).getByText('2 workflows')).toBeInTheDocument();
        expect(within(playgroundGroup).queryByRole('listitem')).not.toBeInTheDocument();

        await userEvent.click(within(playgroundGroup).getByRole('button', {name: 'Toggle workflows'}));

        expect(
            within(playgroundGroup)
                .getAllByRole('listitem')
                .map((item) => item.textContent)
        ).toEqual(['Greet on Request', 'Summarize New Email']);
    });

    it('shows workflows created by the user under a folder header with a No configuration card', () => {
        render(
            <ConnectedUserProjectWorkflowList
                connectedUserProjectWorkflows={[createConnectedUserProjectWorkflow('1', 'Custom Workflow')]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const createdByUserGroup = screen.getByRole('region', {name: 'Created by this user'});

        expect(within(createdByUserGroup).getByRole('heading', {name: 'Created by this user'})).toBeInTheDocument();
        expect(createdByUserGroup.querySelector('svg.lucide-folder')).toBeInTheDocument();
        expect(within(createdByUserGroup).getByText('No configuration')).toBeInTheDocument();
        expect(within(createdByUserGroup).getByText('1 workflow')).toBeInTheDocument();
    });

    it('shows a configured project as a collapsed card with configuration name, version and workflow count', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Default',
                        configurationProjectVersion: 3,
                        projectId: '10',
                    },
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    {
                        ...createConnectedUserProjectWorkflow('1', 'Greet on Request', {
                            automationUuid: 'template-greeting',
                        }),
                        projectId: '10',
                    },
                    createConnectedUserProjectWorkflow('3', 'Custom Workflow'),
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});

        expect(within(playgroundGroup).getByText('Default')).toBeInTheDocument();
        expect(within(playgroundGroup).getByText('V3')).toBeInTheDocument();
        expect(within(playgroundGroup).getByText('1 workflow')).toBeInTheDocument();
        expect(
            within(playgroundGroup).getByRole('button', {name: 'Change configuration Default 10'})
        ).toBeInTheDocument();
        expect(within(playgroundGroup).queryByText('Greet on Request')).not.toBeInTheDocument();

        await userEvent.click(within(playgroundGroup).getByRole('button', {name: 'Toggle configuration'}));

        expect(within(playgroundGroup).getByRole('heading', {name: 'Workflows'})).toBeInTheDocument();
        expect(within(playgroundGroup).getByText('Greet on Request')).toBeInTheDocument();

        const createdByUserGroup = screen.getByRole('region', {name: 'Created by this user'});

        expect(
            within(createdByUserGroup).queryByRole('button', {name: 'Toggle configuration'})
        ).not.toBeInTheDocument();
        expect(
            within(createdByUserGroup).queryByRole('button', {name: /Change configuration/})
        ).not.toBeInTheDocument();
    });

    it('shows the configuration of the source project on a provisioned workflow, which carries no project id of its own', () => {
        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Default',
                        configurationProjectVersion: 3,
                        projectId: '10',
                    },
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    createConnectedUserProjectWorkflow('1', 'Greet on Request', {automationUuid: 'template-greeting'}),
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});

        expect(within(playgroundGroup).getByText('Default')).toBeInTheDocument();
        expect(within(playgroundGroup).queryByText('No configuration')).not.toBeInTheDocument();
        expect(
            within(playgroundGroup).getByRole('button', {name: 'Change configuration Default 10'})
        ).toBeInTheDocument();
    });

    it('passes the project version of its configuration to a provisioned workflow', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Default',
                        configurationProjectVersion: 3,
                        projectId: '10',
                    },
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    {
                        ...createConnectedUserProjectWorkflow('1', 'Greet on Request', {
                            automationUuid: 'template-greeting',
                        }),
                        projectId: '10',
                        workflowVersion: null,
                    },
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});

        await userEvent.click(within(playgroundGroup).getByRole('button', {name: 'Toggle configuration'}));

        expect(within(playgroundGroup).getByText('Greet on Request')).toHaveAttribute(
            'data-configuration-project-version',
            '3'
        );
    });

    it('toggles a configured project card when its row is clicked outside the controls', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Default',
                        configurationProjectVersion: 3,
                        projectId: '10',
                    },
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    {
                        ...createConnectedUserProjectWorkflow('1', 'Greet on Request', {
                            automationUuid: 'template-greeting',
                        }),
                        projectId: '10',
                    },
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const playgroundGroup = screen.getByRole('region', {name: 'Automation Playground'});

        await userEvent.click(within(playgroundGroup).getByText('1 workflow'));

        expect(within(playgroundGroup).getByText('Greet on Request')).toBeInTheDocument();

        await userEvent.click(within(playgroundGroup).getByText('1 workflow'));

        expect(within(playgroundGroup).queryByText('Greet on Request')).not.toBeInTheDocument();
    });

    it('keeps two projects with the same name apart, each with its own configuration', () => {
        const sameNameTemplateSources = new Map<string, ConnectedUserWorkflowTemplateSourceI>([
            [
                'template-first',
                {
                    projectId: '10',
                    projectName: 'Onboarding',
                    workflowTemplate: createWorkflowTemplate('template-first'),
                },
            ],
            [
                'template-second',
                {
                    projectId: '20',
                    projectName: 'Onboarding',
                    workflowTemplate: createWorkflowTemplate('template-second'),
                },
            ],
        ]);

        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Alpha',
                        configurationProjectVersion: 1,
                        projectId: '10',
                    },
                    {configurationId: '61', configurationName: 'Beta', configurationProjectVersion: 2, projectId: '20'},
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    {
                        ...createConnectedUserProjectWorkflow('1', 'First Workflow', {
                            automationUuid: 'template-first',
                        }),
                        projectId: '10',
                    },
                    {
                        ...createConnectedUserProjectWorkflow('2', 'Second Workflow', {
                            automationUuid: 'template-second',
                        }),
                        projectId: '20',
                    },
                ]}
                workflowTemplateSourcesByWorkflowUuid={sameNameTemplateSources}
            />
        );

        const groups = screen.getAllByRole('region', {name: 'Onboarding'});

        expect(groups).toHaveLength(2);
        expect(within(groups[0]).getByText('Alpha')).toBeInTheDocument();
        expect(within(groups[0]).getByText('V1')).toBeInTheDocument();
        expect(within(groups[0]).getByRole('button', {name: 'Change configuration Alpha 10'})).toBeInTheDocument();
        expect(within(groups[1]).getByText('Beta')).toBeInTheDocument();
        expect(within(groups[1]).getByText('V2')).toBeInTheDocument();
        expect(within(groups[1]).getByRole('button', {name: 'Change configuration Beta 20'})).toBeInTheDocument();
    });

    it('shows no configuration or menu on the Created by this user group when it mixes projects', async () => {
        render(
            <ConnectedUserProjectWorkflowList
                configurations={[
                    {
                        configurationId: '51',
                        configurationName: 'Alpha',
                        configurationProjectVersion: 1,
                        projectId: '10',
                    },
                    {configurationId: '61', configurationName: 'Beta', configurationProjectVersion: 2, projectId: '20'},
                ]}
                connectedUser={{connectedUserId: '1050', externalId: 'ext-1050'}}
                connectedUserProjectWorkflows={[
                    {...createConnectedUserProjectWorkflow('1', 'First Custom Workflow'), projectId: '10'},
                    {...createConnectedUserProjectWorkflow('2', 'Second Custom Workflow'), projectId: '20'},
                ]}
                workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
            />
        );

        const createdByUserGroup = screen.getByRole('region', {name: 'Created by this user'});

        await userEvent.click(within(createdByUserGroup).getByRole('button', {name: 'Toggle workflows'}));

        expect(within(createdByUserGroup).getAllByRole('listitem')).toHaveLength(2);
        expect(
            within(createdByUserGroup).queryByRole('button', {name: 'Toggle configuration'})
        ).not.toBeInTheDocument();
        expect(
            within(createdByUserGroup).queryByRole('button', {name: /Change configuration/})
        ).not.toBeInTheDocument();
    });
});
