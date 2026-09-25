import {TooltipProvider} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {ConnectedUserMcpServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserMcpServerListItem from '../ConnectedUserMcpServerListItem';

interface MutationOptionsI {
    onSuccess?: () => void;
}

const hoisted = vi.hoisted(() => ({
    deleteServerMutate: vi.fn(),
    deleteServerOptions: {} as {onSuccess?: () => void},
    enableAutomationMutate: vi.fn(),
    enableAutomationOptions: {} as {onSuccess?: () => void},
    enableServerMutate: vi.fn(),
    enableServerOptions: {} as {onSuccess?: () => void},
    enableToolMutate: vi.fn(),
    enableToolOptions: {} as {onSuccess?: () => void},
    enableWorkflowMutate: vi.fn(),
    enableWorkflowOptions: {} as {onSuccess?: () => void},
    isTenantAdmin: true,
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useAutomationProjectsQuery: () => ({
        data: {
            automationProjects: [
                {
                    id: '20',
                    name: 'Support Desk',
                    workflowTemplates: [
                        {
                            components: [{icon: null, name: 'zendesk', title: 'Zendesk'}],
                            triggers: [{icon: null, name: 'manual', title: 'Manual'}],
                            workflowUuid: 'uuid-1',
                        },
                    ],
                },
            ],
        },
    }),
    useDeleteConnectedUserMcpServerMutation: (options: MutationOptionsI) => {
        hoisted.deleteServerOptions = options;

        return {isPending: false, mutate: hoisted.deleteServerMutate};
    },
    useEnableConnectedUserMcpAutomationMutation: (options: MutationOptionsI) => {
        hoisted.enableAutomationOptions = options;

        return {isPending: false, mutate: hoisted.enableAutomationMutate};
    },
    useEnableConnectedUserMcpServerMutation: (options: MutationOptionsI) => {
        hoisted.enableServerOptions = options;

        return {isPending: false, mutate: hoisted.enableServerMutate};
    },
    useEnableConnectedUserMcpToolMutation: (options: MutationOptionsI) => {
        hoisted.enableToolOptions = options;

        return {isPending: false, mutate: hoisted.enableToolMutate};
    },
    useEnableConnectedUserMcpWorkflowMutation: (options: MutationOptionsI) => {
        hoisted.enableWorkflowOptions = options;

        return {isPending: false, mutate: hoisted.enableWorkflowMutate};
    },
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstances.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/shared/queries/embedded/integrationInstances.queries')>()),
    useGetIntegrationInstanceQuery: (id: number) => ({
        data: {
            id,
            integrationInstanceConfiguration: {integrationVersion: 6, name: 'Gmail Code'},
            integrationInstanceConfigurationId: 77,
        },
    }),
}));

vi.mock('@/shared/hooks/useWorkflowComponentDefinitions', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useWorkflowComponentDefinitions')>()),
    default: () => ({workflowComponentDefinitions: {}, workflowTaskDispatcherDefinitions: {}}),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames?: string[]}) => (
        <div data-testid="trigger-and-components-row">{filteredComponentNames?.join(',')}</div>
    ),
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    useGetWorkflowQuery: (id: string) => ({
        data: {
            id,
            label: id === 'workflow-1' ? 'Send Email' : 'Archive Email',
            workflowTaskComponentNames: id === 'workflow-1' ? ['gmail'] : [],
        },
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery: (integrationId: number, enabled?: boolean) => ({
        data:
            enabled === false
                ? undefined
                : [
                      {status: 'PUBLISHED', version: 6},
                      {status: 'PUBLISHED', version: integrationId === 40 ? 7 : 6},
                      {status: 'DRAFT', version: 8},
                  ],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'gmail', title: 'Gmail'}]}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: {
            clusterElements: [
                {
                    description: "Lists the email messages in the user's mailbox.",
                    name: 'searchEmail',
                    title: 'Search Email',
                    type: 'TOOLS',
                },
            ],
        },
    }),
}));

const connectedUserIntegrationInstances = [{credentialStatus: 'VALID', id: 10}] as ConnectedUserIntegrationInstance[];

const toolsOnlyMcpServer = {
    automations: [],
    enabled: true,
    environmentId: '0',
    id: '1',
    lastModifiedDate: null,
    name: 'Google',
    tools: [
        {
            componentName: 'gmail',
            componentVersion: 1,
            enabled: true,
            id: '100',
            integrationInstanceId: '10',
            name: 'searchEmail',
        },
        {
            componentName: 'gmail',
            componentVersion: 1,
            enabled: false,
            id: '101',
            integrationInstanceId: '10',
            name: 'getEmail',
        },
    ],
    workflows: [],
} as ConnectedUserMcpServer;

const mixedMcpServer = {
    ...toolsOnlyMcpServer,
    workflows: [
        {
            componentName: 'gmail',
            description: 'Sends an email',
            enabled: true,
            id: '201',
            integrationInstanceId: '10',
            integrationVersion: 6,
            lastExecutionDate: '2026-10-05T10:00:00Z',
            name: 'send_email',
            workflowId: 'workflow-1',
        },
        {
            componentName: 'gmail',
            description: null,
            enabled: false,
            id: '202',
            integrationInstanceId: '10',
            integrationVersion: 6,
            lastExecutionDate: null,
            name: 'Archive Email',
            workflowId: 'workflow-2',
        },
    ],
} as ConnectedUserMcpServer;

const automationMcpServer = {
    ...toolsOnlyMcpServer,
    automations: [
        {
            automationName: 'Support Desk',
            available: true,
            connectedUserProjectWorkflowId: '31',
            description: 'Escalates a ticket',
            enabled: true,
            id: '301',
            lastExecutionDate: '2026-10-06T10:00:00Z',
            name: 'Escalate Ticket',
            workflowUuid: 'uuid-1',
        },
        {
            automationName: 'Support Desk',
            available: false,
            connectedUserProjectWorkflowId: '32',
            description: null,
            enabled: false,
            id: '302',
            lastExecutionDate: null,
            name: 'Close Ticket',
            workflowUuid: 'uuid-2',
        },
    ],
} as ConnectedUserMcpServer;

const renderListItem = (
    mcpServer: ConnectedUserMcpServer,
    queryClient = new QueryClient(),
    integrationInstances = connectedUserIntegrationInstances
) =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <ConnectedUserMcpServerListItem
                    connectedUserId={5}
                    connectedUserIntegrationInstances={integrationInstances}
                    mcpServer={mcpServer}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

const expandCard = async () => userEvent.click(screen.getByText('Google'));

const expandGroups = async () => {
    for (const showToolsButton of screen.getAllByRole('button', {name: 'Show tools'})) {
        await userEvent.click(showToolsButton);
    }
};

describe('ConnectedUserMcpServerListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.isTenantAdmin = true;

        useIntegrationInstanceConfigurationWorkflowSheetStore.setState({
            connectedUserId: undefined,
            integrationInstanceConfigurationId: undefined,
            integrationInstanceConfigurationName: undefined,
            integrationInstanceConfigurationWorkflowSheetOpen: false,
            integrationVersion: undefined,
            workflow: undefined,
        });
    });

    it('counts component and workflow tools in the card header', () => {
        renderListItem(mixedMcpServer);

        expect(screen.getByText('2 component tools')).toBeInTheDocument();
        expect(screen.getByText('2 workflow tools')).toBeInTheDocument();
    });

    it('shows the latest workflow execution in the card header', () => {
        renderListItem(mixedMcpServer);

        const lastExecutionDate = new Date('2026-10-05T10:00:00Z');

        expect(
            screen.getByText(
                `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
            )
        ).toBeInTheDocument();
    });

    it('shows no executions in the card header of a server whose workflows never ran', () => {
        renderListItem(toolsOnlyMcpServer);

        expect(screen.getByText('No executions')).toBeInTheDocument();
    });

    it('shows the latest execution of a workflow tool group and none for component tool groups', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();

        const workflowGroup = screen.getByText('V6').closest('[data-slot="collapsible"]')!;
        const componentGroup = screen.getByText('V1').closest('[data-slot="collapsible"]')!;

        expect(within(workflowGroup as HTMLElement).getByText(/^Executed at /)).toBeInTheDocument();
        expect(
            within(componentGroup as HTMLElement).queryByText(/^Executed at |No executions/)
        ).not.toBeInTheDocument();
    });

    it('groups tools by component, collapsed, with version badges and credential status', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();

        expect(screen.getByText('Component Tools')).toBeInTheDocument();
        expect(screen.getByText('Workflow Tools')).toBeInTheDocument();
        expect(screen.getByText('V1')).toBeInTheDocument();
        expect(screen.getByText('V6')).toBeInTheDocument();
        expect(screen.getAllByText('Account Connected')).toHaveLength(2);
        expect(screen.queryByText('Search Email')).not.toBeInTheDocument();
    });

    it('names a workflow tool after its workflow, as the MCP server page does', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByRole('button', {name: 'Send Email'})).toBeInTheDocument();
        expect(screen.queryByText('send_email')).not.toBeInTheDocument();
    });

    it("opens a workflow's executions for the connected user when the workflow is clicked", async () => {
        renderListItem(mixedMcpServer);

        await expandCard();
        await expandGroups();

        expect(
            screen.getByRole('button', {name: 'Send Email'}).closest('li')?.querySelector('svg.lucide-workflow')
        ).toBeNull();

        await userEvent.click(screen.getByRole('button', {name: 'Send Email'}));

        const state = useIntegrationInstanceConfigurationWorkflowSheetStore.getState();

        expect(state.integrationInstanceConfigurationWorkflowSheetOpen).toBe(true);
        expect(state.connectedUserId).toBe(5);
        expect(state.integrationInstanceConfigurationId).toBe(77);
        expect(state.integrationInstanceConfigurationName).toBe('Gmail Code');
        expect(state.integrationVersion).toBe(6);
        expect(state.workflow).toEqual({
            id: 'workflow-1',
            label: 'Send Email',
            workflowTaskComponentNames: ['gmail'],
        });
        expect(screen.getAllByTestId('trigger-and-components-row')[0]).toHaveTextContent('gmail');
    });

    it('shows tool titles and descriptions and toggles a tool for the user', async () => {
        renderListItem(toolsOnlyMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByText('Search Email')).toBeInTheDocument();
        expect(screen.getByText("Lists the email messages in the user's mailbox.")).toBeInTheDocument();
        expect(screen.getByText('getEmail')).toBeInTheDocument();

        const getEmailRow = screen.getByText('getEmail').closest('li')!;

        await userEvent.click(within(getEmailRow).getByRole('switch'));

        expect(hoisted.enableToolMutate).toHaveBeenCalledWith({enable: true, id: '101'});
    });

    it('switches a workflow with the MCP workflow mutation', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.queryByText('Sends an email')).not.toBeInTheDocument();
        expect(screen.getAllByText(/^Executed at /)).toHaveLength(3);
        expect(screen.getByText('No executions')).toBeInTheDocument();

        const archiveEmailRow = screen.getByText('Archive Email').closest('li')!;

        await userEvent.click(within(archiveEmailRow).getByRole('switch'));

        expect(hoisted.enableWorkflowMutate).toHaveBeenCalledWith({enable: true, id: '202'});
    });

    it('lets the admin delete a server that exposes workflows', async () => {
        renderListItem(mixedMcpServer);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Delete'})).not.toHaveAttribute('aria-disabled');
    });

    it('toggles the whole server and refreshes the Integrations tab data', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(mixedMcpServer, queryClient);

        await userEvent.click(screen.getAllByRole('switch')[0]);

        expect(hoisted.enableServerMutate).toHaveBeenCalledWith({
            connectedUserId: '5',
            enable: false,
            mcpServerId: '1',
        });

        hoisted.enableServerOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ConnectedUserKeys.connectedUser(5)});
    });

    it('deletes the server for the user after confirmation and refreshes the list and what can be provisioned again', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(toolsOnlyMcpServer, queryClient);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteServerMutate).toHaveBeenCalledWith({connectedUserId: '5', mcpServerId: '1'});

        hoisted.deleteServerOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['provisionableConnectedUserMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['provisionableConnectedUserMcpServers']});
    });

    it('refreshes the MCP servers and Integrations data after a tool or workflow toggle', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(mixedMcpServer, queryClient);

        await expandCard();
        await expandGroups();

        hoisted.enableToolOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});

        invalidateQueriesSpy.mockClear();

        hoisted.enableWorkflowOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
    });

    it('renders the automation group with a switch per automation', async () => {
        renderListItem(automationMcpServer);

        expect(screen.getByText('2 automation tools')).toBeInTheDocument();

        await expandCard();

        await expandGroups();

        expect(screen.getByText('Automation Tools')).toBeInTheDocument();

        const escalateRow = screen.getByText('Escalate Ticket').closest('li')!;
        const closeRow = screen.getByText('Close Ticket').closest('li')!;

        expect(within(escalateRow).getByRole('switch')).toBeChecked();
        expect(within(closeRow).getByRole('switch')).not.toBeChecked();
    });

    it('greys out an unavailable automation and cannot switch it on', async () => {
        renderListItem(automationMcpServer);

        await expandCard();

        await expandGroups();

        const escalateRow = screen.getByText('Escalate Ticket').closest('li')!;
        const closeRow = screen.getByText('Close Ticket').closest('li')!;

        expect(closeRow.firstElementChild?.className).toContain('opacity-50');
        expect(within(closeRow).getByRole('switch')).toBeDisabled();
        expect(escalateRow.firstElementChild?.className).not.toContain('opacity-50');
        expect(within(escalateRow).getByRole('switch')).toBeEnabled();
    });

    it('explains an unavailable automation as not available to this user', async () => {
        renderListItem(automationMcpServer);

        await expandCard();

        await expandGroups();

        await userEvent.hover(screen.getByText('Close Ticket'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Not available to this user');
    });

    it('explains the disabled switch of an unavailable automation as not available to this user', async () => {
        renderListItem(automationMcpServer);

        await expandCard();

        await expandGroups();

        const closeRow = screen.getByText('Close Ticket').closest('li')!;

        await userEvent.hover(within(closeRow).getByRole('switch').parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Not available to this user');
    });

    it('toggles an automation with the enable MCP automation mutation only', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(automationMcpServer, queryClient);

        await expandCard();

        await expandGroups();

        const escalateRow = screen.getByText('Escalate Ticket').closest('li')!;

        await userEvent.click(within(escalateRow).getByRole('switch'));

        expect(hoisted.enableAutomationMutate).toHaveBeenCalledWith({
            connectedUserId: '5',
            enable: false,
            id: '301',
        });

        hoisted.enableAutomationOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueriesSpy).not.toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
    });

    it('lets an enabled but unavailable automation be switched off', async () => {
        renderListItem({
            ...automationMcpServer,
            automations: [{...automationMcpServer.automations[1], enabled: true}],
        });

        await expandCard();

        await expandGroups();

        const closeRow = screen.getByText('Close Ticket').closest('li')!;
        const closeSwitch = within(closeRow).getByRole('switch');

        expect(closeSwitch).toBeEnabled();

        await userEvent.click(closeSwitch);

        expect(hoisted.enableAutomationMutate).toHaveBeenCalledWith({
            connectedUserId: '5',
            enable: false,
            id: '302',
        });
    });

    it('groups automation tools under their project', async () => {
        renderListItem({
            ...automationMcpServer,
            automations: [
                ...automationMcpServer.automations,
                {...automationMcpServer.automations[0], automationName: 'Billing', id: '303', name: 'Refund Order'},
            ],
        });

        await expandCard();

        const supportDeskGroup = screen.getByRole('group', {name: 'Support Desk'});
        const billingGroup = screen.getByRole('group', {name: 'Billing'});

        await expandGroups();

        expect(within(supportDeskGroup).getByText('Escalate Ticket')).toBeInTheDocument();
        expect(within(supportDeskGroup).getByText('Close Ticket')).toBeInTheDocument();
        expect(within(billingGroup).getByText('Refund Order')).toBeInTheDocument();
        expect(within(billingGroup).queryByText('Escalate Ticket')).not.toBeInTheDocument();
    });

    it('shows the trigger and components of an automation tool from its project workflow', async () => {
        renderListItem(automationMcpServer);

        await expandCard();

        await expandGroups();

        const escalateRow = screen.getByText('Escalate Ticket').closest('li')!;
        const closeRow = screen.getByText('Close Ticket').closest('li')!;

        expect(within(escalateRow).getByTestId('trigger-and-components-row')).toHaveTextContent('manual,zendesk');
        expect(within(closeRow).queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
    });

    it('marks a workflow tool group whose integration has a newer published version', async () => {
        renderListItem(mixedMcpServer, new QueryClient(), [
            {credentialStatus: 'VALID', id: 10, integrationId: 40},
        ] as ConnectedUserIntegrationInstance[]);

        await expandCard();

        expect(screen.getAllByRole('img', {name: 'Newer version available'})).toHaveLength(1);
    });

    it('does not mark a workflow tool group already on the last published version', async () => {
        renderListItem(mixedMcpServer, new QueryClient(), [
            {credentialStatus: 'VALID', id: 10, integrationId: 41},
        ] as ConnectedUserIntegrationInstance[]);

        await expandCard();

        expect(screen.queryByRole('img', {name: 'Newer version available'})).not.toBeInTheDocument();
    });

    it('shows the server and its component tools read-only to a user who is not a tenant admin', async () => {
        hoisted.isTenantAdmin = false;

        renderListItem(toolsOnlyMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByText('getEmail')).toBeInTheDocument();
        expect(screen.getAllByRole('switch')).toHaveLength(3);
        screen.getAllByRole('switch').forEach((switchElement) => expect(switchElement).toBeDisabled());
        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();
    });
});
