import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProvisionProjectWorkflowDialog from '../ProvisionProjectWorkflowDialog';

const hoisted = vi.hoisted(() => ({
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
        hoisted.mutate.mockReset();
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

        expect(screen.getByLabelText('Configuration')).toHaveValue('51');
    });

    it('lists workflows of the selected configuration', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');

        expect(screen.getByRole('checkbox', {name: 'Welcome'})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: 'Beta only'})).not.toBeInTheDocument();

        await userEvent.selectOptions(screen.getByLabelText('Configuration'), '50');

        expect(screen.getByRole('checkbox', {name: 'Beta only'})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', {name: 'Welcome'})).not.toBeInTheDocument();
    });

    it('sends configurationId when provisioning', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.selectOptions(screen.getByLabelText('Configuration'), '50');
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
});
