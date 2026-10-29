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

const PROJECTS = [
    {
        name: 'Onboarding',
        projectId: '10',
        projectVersion: 3,
        workflows: [
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
        ],
    },
];

const renderDialog = (onClose = vi.fn(), queryClient = new QueryClient()) => {
    render(
        <QueryClientProvider client={queryClient}>
            <ProvisionProjectWorkflowDialog connectedUserId={7} onClose={onClose} />
        </QueryClientProvider>
    );

    return {onClose, queryClient};
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
            connectedUserId: '7',
            workflows: [{inputs: {name: 'x'}, workflowUuid: 'wf-1'}],
        });

        hoisted.mutationOptions.onSuccess?.();

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['provisionableConnectedUserProjects']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUsers', 7]});
        expect(onClose).toHaveBeenCalled();
    });

    it('keeps dotted input names as literal keys', async () => {
        renderDialog();

        await userEvent.selectOptions(screen.getByLabelText('Project'), '10');
        await userEvent.click(screen.getByRole('checkbox', {name: 'Dotted'}));
        await userEvent.type(screen.getByLabelText(/Email/), 'a@b.c');

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            connectedUserId: '7',
            workflows: [{inputs: {'user.email': 'a@b.c'}, workflowUuid: 'wf-4'}],
        });
    });
});
