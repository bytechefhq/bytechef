import ConnectedUserIntegrationWorkflowEditDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationWorkflowEditDialog';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    onClose: vi.fn(),
    updateConnection: vi.fn(),
    updateWorkflow: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations', () => ({
    useUpdateIntegrationInstanceWorkflowMutation: () => ({isPending: false, mutateAsync: hoisted.updateWorkflow}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstances.mutations', () => ({
    useUpdateIntegrationInstanceConnectionMutation: () => ({isPending: false, mutateAsync: hoisted.updateConnection}),
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserNewConnectionDialog', () => ({
    default: ({
        componentName,
        onConnectionCreate,
    }: {
        componentName: string;
        onConnectionCreate: (connectionId: number) => void;
    }) => (
        <button onClick={() => onConnectionCreate(99)} type="button">
            {`Create ${componentName} connection`}
        </button>
    ),
}));

const connectionHoisted = vi.hoisted(() => ({
    environmentConnections: [{componentName: 'gmail', id: 21, name: 'Support Gmail'}],
    userConnections: [
        {componentName: 'cevisio', id: 11, name: 'Jane cevisio', origin: 'PROVISIONED'},
        {componentName: 'cevisio', id: 12, name: 'Jane cevisio backup', origin: 'SELF_SERVE'},
        {componentName: 'gmail', id: 13, name: 'Jane Gmail', origin: 'SELF_SERVE'},
    ],
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {name: 'cevisio', title: 'cevisio'},
            {name: 'gmail', title: 'Gmail'},
        ],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connections: ['connections']},
    useGetAllConnectedUserConnectionsQuery: () => ({data: connectionHoisted.userConnections}),
    useGetConnectionsQuery: () => ({data: connectionHoisted.environmentConnections}),
}));

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: ({
        control,
        inputs,
    }: {
        control: {register: (name: string) => Record<string, unknown>};
        inputs: Array<{name: string}>;
    }) => (
        <ul>
            {inputs.map((input) => (
                <li key={input.name}>
                    {`input ${input.name}`}

                    <input aria-label={input.name} {...control.register(`inputs.${input.name}`)} />
                </li>
            ))}
        </ul>
    ),
}));

const workflow = {
    id: 'workflow-1',
    inputs: [
        {internalOnly: true, name: 'contactPageUrl'},
        {internalOnly: false, name: 'channel'},
    ],
    label: 'Rate Ticket from Email',
} as unknown as Workflow;

let queryClient: QueryClient;

const renderDialog = (inputs: Workflow['inputs'] = workflow.inputs, provisioned = false) =>
    render(
        <QueryClientProvider client={queryClient}>
            <ConnectedUserIntegrationWorkflowEditDialog
                integrationInstance={{connectedUserId: 3, connectionId: 11, id: 7, name: 'instance'} as never}
                integrationInstanceConfiguration={
                    {environmentId: 1, id: 4, integration: {componentName: 'cevisio'}, name: 'Development'} as never
                }
                integrationInstanceConfigurationWorkflow={
                    {
                        connections: [{connectionId: 21, workflowConnectionKey: 'gmail', workflowNodeName: 'gmail_1'}],
                        id: 1,
                    } as never
                }
                integrationInstanceWorkflow={{
                    enabled: true,
                    inputs: {channel: 'general', contactPageUrl: 'https://vendor'},
                    integrationInstanceConfigurationWorkflowId: 1,
                    workflowId: 'workflow-1',
                }}
                onClose={hoisted.onClose}
                provisioned={provisioned}
                workflow={{...workflow, inputs}}
            />
        </QueryClientProvider>
    );

describe('ConnectedUserIntegrationWorkflowEditDialog', () => {
    beforeEach(() => {
        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        queryClient = new QueryClient();

        hoisted.onClose.mockReset();
        hoisted.updateConnection.mockReset();
        hoisted.updateConnection.mockResolvedValue(undefined);
        hoisted.updateWorkflow.mockReset();
        hoisted.updateWorkflow.mockResolvedValue(undefined);
    });

    it("shows the user's account connection and the admin's internal connections read-only", async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));

        expect(screen.getByLabelText('cevisio')).toHaveValue('Jane cevisio');
        expect(screen.getByLabelText('cevisio')).toHaveAttribute('readonly');
        expect(screen.getByLabelText('Gmail')).toHaveValue('Support Gmail');
        expect(screen.getByText(/Internal connection for gmail_1, set by an admin/)).toBeInTheDocument();
    });

    it('lists only the inputs a connected user sets', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.getByText('input channel')).toBeInTheDocument();
        expect(screen.queryByText('input contactPageUrl')).not.toBeInTheDocument();
    });

    it('saves the instance inputs, keeping the stored values it does not show', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateConnection).not.toHaveBeenCalled();
        expect(hoisted.updateWorkflow).toHaveBeenCalledWith({
            id: 7,
            updateIntegrationInstanceWorkflowRequest: {
                inputs: {channel: 'general', contactPageUrl: 'https://vendor'},
            },
            workflowId: 'workflow-1',
        });
    });

    it('explains where internal-only inputs are set when the workflow has no other inputs', async () => {
        renderDialog([{internalOnly: true, name: 'contactPageUrl'}] as Workflow['inputs']);

        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.getByText(/Internal-only inputs are set on the integration configuration/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it("lets an admin pick another of the connected user's connections of the integration's component when it is provisioned", async () => {
        renderDialog(workflow.inputs, true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));

        expect(screen.getByRole('combobox', {name: 'cevisio connection'})).toHaveTextContent('Jane cevisio');
        expect(screen.getByText(/A change applies to every workflow of this integration/)).toBeInTheDocument();
        expect(screen.getByLabelText('Gmail')).toHaveAttribute('readonly');

        await userEvent.click(screen.getByRole('combobox', {name: 'cevisio connection'}));

        expect(screen.getByRole('option', {name: /Jane cevisio backup/})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /Jane Gmail/})).not.toBeInTheDocument();
    });

    it('switches the connection before saving the inputs and closes once both are saved', async () => {
        renderDialog(workflow.inputs, true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));
        await userEvent.click(screen.getByRole('combobox', {name: 'cevisio connection'}));
        await userEvent.click(screen.getByRole('option', {name: /Jane cevisio backup/}));
        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));
        await userEvent.type(screen.getByRole('textbox', {name: 'channel'}), '-ops');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateConnection).toHaveBeenCalledWith({
            id: 7,
            updateIntegrationInstanceConnectionRequest: {connectionId: 12},
        });
        expect(hoisted.updateWorkflow).toHaveBeenCalled();
        expect(hoisted.updateConnection.mock.invocationCallOrder[0]).toBeLessThan(
            hoisted.updateWorkflow.mock.invocationCallOrder[0]
        );
        expect(hoisted.onClose).toHaveBeenCalled();
    });

    it('switches only the connection when the inputs are unchanged, so the triggers restart once', async () => {
        renderDialog(workflow.inputs, true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));
        await userEvent.click(screen.getByRole('combobox', {name: 'cevisio connection'}));
        await userEvent.click(screen.getByRole('option', {name: /Jane cevisio backup/}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateConnection).toHaveBeenCalled();
        expect(hoisted.updateWorkflow).not.toHaveBeenCalled();
        expect(hoisted.onClose).toHaveBeenCalled();
    });

    it('refreshes the connected user after the connection switch even when the inputs then fail to save', async () => {
        hoisted.updateWorkflow.mockRejectedValue(new Error('inputs failed'));

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderDialog(workflow.inputs, true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));
        await userEvent.click(screen.getByRole('combobox', {name: 'cevisio connection'}));
        await userEvent.click(screen.getByRole('option', {name: /Jane cevisio backup/}));
        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));
        await userEvent.type(screen.getByRole('textbox', {name: 'channel'}), '-ops');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateWorkflow).toHaveBeenCalled();
        expect(hoisted.onClose).not.toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connections']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUsers', 3]});
    });

    it('saves a changed connection even when the workflow has no inputs to set', async () => {
        renderDialog([{internalOnly: true, name: 'contactPageUrl'}] as Workflow['inputs'], true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'New cevisio connection'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create cevisio connection'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateConnection).toHaveBeenCalledWith({
            id: 7,
            updateIntegrationInstanceConnectionRequest: {connectionId: 99},
        });
        expect(hoisted.updateWorkflow).not.toHaveBeenCalled();
    });

    it('keeps the dialog open and skips the inputs when the connection switch fails', async () => {
        hoisted.updateConnection.mockRejectedValue(new Error('restart failed'));

        renderDialog(workflow.inputs, true);

        await userEvent.click(screen.getByRole('tab', {name: /Connections/}));
        await userEvent.click(screen.getByRole('combobox', {name: 'cevisio connection'}));
        await userEvent.click(screen.getByRole('option', {name: /Jane cevisio backup/}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateWorkflow).not.toHaveBeenCalled();
        expect(hoisted.onClose).not.toHaveBeenCalled();
    });
});
