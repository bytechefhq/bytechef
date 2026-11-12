import {ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProvisionProjectWorkflowDialog from '../ProvisionProjectWorkflowDialog';

const hoisted = vi.hoisted(() => ({
    connectionsQuery: vi.fn(),
    mutate: vi.fn(),
    mutationOptions: {} as {onSuccess?: () => void},
    provisionableQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useProvisionConnectedUserProjectWorkflowsMutation: (options: {onSuccess?: () => void}) => {
        hoisted.mutationOptions = options;

        return {isPending: false, mutate: hoisted.mutate};
    },
    useProvisionableConnectedUserProjectsQuery: hoisted.provisionableQuery,
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserNewConnectionDialog', () => ({
    default: ({
        componentName,
        connectedUserId,
        onConnectionCreate,
    }: {
        componentName: string;
        connectedUserId: number;
        onConnectionCreate: (connectionId: number) => void;
    }) => (
        <button onClick={() => onConnectionCreate(99)} type="button">
            {`Create ${componentName} connection for user ${connectedUserId}`}
        </button>
    ),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    useGetAllConnectedUserConnectionsQuery: hoisted.connectionsQuery,
}));

const SLACK_CONNECTIONS = [
    {componentName: 'slack', id: 31, name: 'Own Slack', origin: ConnectionOriginEnum.SelfServe},
    {componentName: 'slack', id: 32, name: 'Shared Slack', origin: ConnectionOriginEnum.Internal},
    {componentName: 'gmail', id: 40, name: 'Own Gmail', origin: ConnectionOriginEnum.SelfServe},
];

const WORKFLOWS = [
    {
        dangling: false,
        inputs: [{label: 'Name', name: 'name', required: true, type: 'string'}],
        label: 'Welcome',
        provisioned: false,
        workflowUuid: 'wf-1',
    },
    {
        dangling: false,
        inputs: [{label: 'Email', name: 'user.email', required: true, type: 'string'}],
        label: 'Dotted',
        provisioned: false,
        workflowUuid: 'wf-4',
    },
    {dangling: false, inputs: [], label: 'Already there', provisioned: true, workflowUuid: 'wf-2'},
    {dangling: true, inputs: [], label: 'Broken', provisioned: true, workflowUuid: 'wf-3'},
    {
        components: [{icon: null, name: 'slack', title: 'Slack'}],
        dangling: false,
        inputs: [],
        label: 'Notify',
        provisioned: false,
        workflowUuid: 'wf-6',
    },
];

const SLACK_WORKFLOWS = [
    {
        components: [{icon: null, name: 'slack', title: 'Slack'}],
        dangling: false,
        inputs: [{label: 'Channel', name: 'channel', required: false, type: 'string'}],
        label: 'Digest',
        provisioned: false,
        workflowUuid: 'wf-7',
    },
    {
        components: [{icon: null, name: 'slack', title: 'Slack'}],
        dangling: false,
        inputs: [],
        label: 'Alert',
        provisioned: false,
        workflowUuid: 'wf-8',
    },
];

const SLACK_PROJECTS = [
    {
        assignedConfigurationId: null,
        configurations: [{id: '60', name: 'Slack', projectVersion: 1, selfServe: true, workflows: SLACK_WORKFLOWS}],
        name: 'Slack Project',
        projectId: '20',
        projectVersion: 1,
        workflows: SLACK_WORKFLOWS,
    },
];

const BETA_WORKFLOWS = [{dangling: false, inputs: [], label: 'Beta only', provisioned: false, workflowUuid: 'wf-5'}];

const PROJECTS = [
    {
        assignedConfigurationId: null,
        configurations: [
            {id: '50', name: 'Beta', projectVersion: 2, selfServe: false, workflows: BETA_WORKFLOWS},
            {id: '51', name: 'Default', projectVersion: 3, selfServe: true, workflows: WORKFLOWS},
        ],
        name: 'Onboarding',
        projectId: '10',
        projectVersion: 3,
        workflows: WORKFLOWS,
    },
];

const renderDialog = (
    onClose = vi.fn(),
    queryClient = new QueryClient(),
    assignedConfigurations: Array<{configurationId: string; configurationName: string; projectId: string}> = []
) => {
    const dialog = (
        <QueryClientProvider client={queryClient}>
            <ProvisionProjectWorkflowDialog
                assignedConfigurations={assignedConfigurations}
                connectedUserId={7}
                onClose={onClose}
            />
        </QueryClientProvider>
    );

    const {rerender} = render(dialog);

    return {onClose, queryClient, rerenderDialog: () => rerender(dialog)};
};

describe('ProvisionProjectWorkflowDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        hoisted.mutate.mockReset();
        hoisted.connectionsQuery.mockReturnValue({data: []});
        hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserProjects: PROJECTS}});
    });

    it('shows the empty state when no project is provisionable', () => {
        hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserProjects: []}});

        renderDialog();

        expect(
            screen.getByText("No project has a configuration released to this user's environment.")
        ).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Close'}).some((button) => button.textContent === 'Close')).toBe(
            true
        );
        expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();
    });

    it('shows each project with its version', () => {
        renderDialog();

        expect(screen.getByRole('option', {name: 'Onboarding V3'})).toBeInTheDocument();
    });

    it("lists the chosen project's workflows and disables provisioned ones", async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByText('Welcome')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: 'Welcome'})).toBeEnabled();
        expect(screen.getByRole('checkbox', {name: 'Already there'})).toBeDisabled();
        expect(screen.getByRole('checkbox', {name: 'Already there'})).toBeChecked();
        expect(screen.getByText('Provisioned')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: 'Broken'})).toBeDisabled();
        expect(screen.getByText('Needs attention')).toBeInTheDocument();
    });

    it('keeps Save disabled until a workflow is selected and its required inputs are filled', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Welcome'}));

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.type(screen.getByLabelText(/Name/), 'x');

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());
    });

    it('submits the selected workflows with their inputs and closes', async () => {
        const {onClose, queryClient} = renderDialog();

        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('checkbox', {name: 'Welcome'}));
        await userEvent.type(screen.getByLabelText(/Name/), 'x');

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '51',
            connectedUserId: '7',
            workflows: [{inputs: {name: 'x'}, workflowUuid: 'wf-1'}],
        });

        hoisted.mutationOptions.onSuccess?.();

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['provisionableConnectedUserProjects']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUsers', 7]});
        expect(onClose).toHaveBeenCalled();
    });

    it('keeps the selection and inputs on the same workflow when a refetch reorders the list', async () => {
        const {rerenderDialog} = renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('checkbox', {name: 'Welcome'}));
        await userEvent.type(screen.getByLabelText(/Name/), 'x');

        const [project] = PROJECTS;

        hoisted.provisionableQuery.mockReturnValue({
            data: {
                provisionableConnectedUserProjects: [
                    {
                        ...project,
                        configurations: project.configurations.map((configuration) => ({
                            ...configuration,
                            workflows: [...configuration.workflows].reverse(),
                        })),
                    },
                ],
            },
        });

        rerenderDialog();

        expect(screen.getByRole('checkbox', {name: 'Welcome'})).toBeChecked();
        expect(screen.getByRole('checkbox', {name: 'Dotted'})).not.toBeChecked();
        expect(screen.getByLabelText(/Name/)).toHaveValue('x');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '51',
            connectedUserId: '7',
            workflows: [{inputs: {name: 'x'}, workflowUuid: 'wf-1'}],
        });
    });

    it('keeps dotted input names as literal keys', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('checkbox', {name: 'Dotted'}));
        await userEvent.type(screen.getByLabelText(/Email/), 'a@b.c');

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '51',
            connectedUserId: '7',
            workflows: [{inputs: {'user.email': 'a@b.c'}, workflowUuid: 'wf-4'}],
        });
    });

    it('preselects the self-serve configuration', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        const configurationSelect = screen.getByRole('combobox', {name: 'Configuration'});

        expect(within(configurationSelect).getByText('Default')).toBeInTheDocument();
        expect(within(configurationSelect).getByText('V3')).toBeInTheDocument();
    });

    it('shows each configuration version as a badge after its name', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

        const betaOption = await screen.findByRole('option', {name: /Beta/});

        expect(within(betaOption).getByText('Beta')).toBeInTheDocument();
        expect(within(betaOption).getByText('V2')).toBeInTheDocument();
    });

    it('lists workflows of the selected configuration', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByRole('checkbox', {name: 'Welcome'})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: 'Beta only'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));
        await userEvent.click(await screen.findByRole('option', {name: /Beta/}));

        expect(screen.getByRole('checkbox', {name: 'Beta only'})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: 'Welcome'})).not.toBeInTheDocument();
    });

    it('sends configurationId when provisioning', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));
        await userEvent.click(await screen.findByRole('option', {name: /Beta/}));
        await userEvent.click(screen.getByRole('checkbox', {name: 'Beta only'}));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '50',
            connectedUserId: '7',
            workflows: [{workflowUuid: 'wf-5'}],
        });
    });

    it('locks the configuration for an assigned user', async () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: [{...PROJECTS[0], assignedConfigurationId: '50'}]},
        });

        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByLabelText('Configuration')).toHaveValue('Beta · v2');
        expect(screen.getByLabelText('Configuration')).toHaveAttribute('readonly');
        expect(screen.getByText('Use Change configuration to move this user.')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: 'Beta only'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('checkbox', {name: 'Beta only'}));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '50',
            connectedUserId: '7',
            workflows: [{workflowUuid: 'wf-5'}],
        });
    });

    it('shows the assigned name and blocks provisioning when the assigned configuration is disabled', async () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: [{...PROJECTS[0], assignedConfigurationId: '99'}]},
        });

        renderDialog(vi.fn(), new QueryClient(), [
            {configurationId: '99', configurationName: 'Paused', projectId: '10'},
        ]);

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByLabelText('Configuration')).toHaveValue('Paused');
        expect(screen.getByText(/this configuration is disabled/i)).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('falls back to a generic label when the disabled assigned configuration is unknown', async () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: [{...PROJECTS[0], assignedConfigurationId: '99'}]},
        });

        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByLabelText('Configuration')).toHaveValue('Assigned configuration (disabled)');
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('says so and disables Save when no configuration is available and none is assigned', async () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: [{...PROJECTS[0], configurations: []}]},
        });

        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByText('No configuration available for this user')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    describe('connections', () => {
        it('preselects the first connection of each component the selected workflows need and sends it', async () => {
            hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Notify'}));

            const slackConnectionSelect = screen.getByRole('combobox', {name: 'Slack connection'});

            expect(within(slackConnectionSelect).getByText('Own Slack')).toBeInTheDocument();

            await userEvent.click(slackConnectionSelect);

            const options = await screen.findAllByRole('option');

            expect(options).toHaveLength(2);
            expect(within(options[0]).getByText('Own Slack')).toBeInTheDocument();
            expect(within(options[0]).getByText('Self-created')).toBeInTheDocument();
            expect(within(options[1]).getByText('Shared Slack')).toBeInTheDocument();
            expect(within(options[1]).getByText('Internal')).toBeInTheDocument();

            await userEvent.keyboard('{Escape}');

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.mutate).toHaveBeenCalledWith({
                configurationId: '51',
                connectedUserId: '7',
                workflows: [{connections: [{componentName: 'slack', connectionId: '31'}], workflowUuid: 'wf-6'}],
            });
        });

        it('sends the connection the admin picks', async () => {
            hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Notify'}));
            await userEvent.click(screen.getByRole('combobox', {name: 'Slack connection'}));
            await userEvent.click(await screen.findByRole('option', {name: /Shared Slack/}));
            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.mutate).toHaveBeenCalledWith({
                configurationId: '51',
                connectedUserId: '7',
                workflows: [{connections: [{componentName: 'slack', connectionId: '32'}], workflowUuid: 'wf-6'}],
            });
        });

        it('warns that workflows are provisioned disabled when the user has no connection for a component', async () => {
            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Notify'}));

            expect(
                screen.getByText(
                    'No Slack connection. Create one, or workflows that use Slack will be provisioned disabled.'
                )
            ).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'New Slack connection'})).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.mutate).toHaveBeenCalledWith({
                configurationId: '51',
                connectedUserId: '7',
                workflows: [{workflowUuid: 'wf-6'}],
            });
        });

        it('creates a connection for the connected user and selects it', async () => {
            hoisted.connectionsQuery.mockReturnValue({
                data: [
                    ...SLACK_CONNECTIONS,
                    {componentName: 'slack', id: 99, name: 'New Slack', origin: ConnectionOriginEnum.Provisioned},
                ],
            });

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Notify'}));
            await userEvent.click(screen.getByRole('button', {name: 'New Slack connection'}));
            await userEvent.click(screen.getByRole('button', {name: 'Create slack connection for user 7'}));

            expect(
                within(screen.getByRole('combobox', {name: 'Slack connection'})).getByText('New Slack')
            ).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Create slack connection for user 7'})).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.mutate).toHaveBeenCalledWith({
                configurationId: '51',
                connectedUserId: '7',
                workflows: [{connections: [{componentName: 'slack', connectionId: '99'}], workflowUuid: 'wf-6'}],
            });
        });

        it("shows a workflow's connections and then its inputs inside the workflow's box", async () => {
            hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});
            hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserProjects: SLACK_PROJECTS}});

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '20');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Digest'}));

            const workflowBox = within(screen.getByRole('group', {name: 'Digest'}));

            const connectionsHeading = workflowBox.getByText('Connections');
            const connectionSelect = workflowBox.getByRole('combobox', {name: 'Slack connection'});
            const inputsHeading = workflowBox.getByText('Inputs');
            const channelInput = workflowBox.getByLabelText('Channel');

            expect(connectionsHeading.compareDocumentPosition(connectionSelect)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
            expect(connectionSelect.compareDocumentPosition(inputsHeading)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
            expect(inputsHeading.compareDocumentPosition(channelInput)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        });

        it('sends each workflow the connection picked for it', async () => {
            hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});
            hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserProjects: SLACK_PROJECTS}});

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '20');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Digest'}));
            await userEvent.click(screen.getByRole('checkbox', {name: 'Alert'}));
            await userEvent.click(
                within(screen.getByRole('group', {name: 'Alert'})).getByRole('combobox', {name: 'Slack connection'})
            );
            await userEvent.click(await screen.findByRole('option', {name: /Shared Slack/}));
            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.mutate).toHaveBeenCalledWith({
                configurationId: '60',
                connectedUserId: '7',
                workflows: [
                    {
                        connections: [{componentName: 'slack', connectionId: '31'}],
                        inputs: {},
                        workflowUuid: 'wf-7',
                    },
                    {connections: [{componentName: 'slack', connectionId: '32'}], workflowUuid: 'wf-8'},
                ],
            });
        });

        it('shows no connection choice while the selected workflows need none', async () => {
            hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});

            renderDialog();

            await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
            await userEvent.click(screen.getByRole('checkbox', {name: 'Welcome'}));

            expect(screen.queryByRole('combobox', {name: 'Slack connection'})).not.toBeInTheDocument();
        });
    });
});
