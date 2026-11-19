import ConnectedUserIntegrationWorkflowEditDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationWorkflowEditDialog';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({mutate: vi.fn()}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations', () => ({
    useUpdateIntegrationInstanceWorkflowMutation: () => ({isPending: false, mutate: hoisted.mutate}),
}));

const connectionHoisted = vi.hoisted(() => ({
    environmentConnections: [{componentName: 'gmail', id: 21, name: 'Support Gmail'}],
    userConnections: [{componentName: 'cevisio', id: 11, name: 'Jane cevisio'}],
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
    useGetAllConnectedUserConnectionsQuery: () => ({data: connectionHoisted.userConnections}),
    useGetConnectionsQuery: () => ({data: connectionHoisted.environmentConnections}),
}));

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: ({inputs}: {inputs: Array<{name: string}>}) => (
        <ul>
            {inputs.map((input) => (
                <li key={input.name}>{`input ${input.name}`}</li>
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

const renderDialog = (inputs: Workflow['inputs'] = workflow.inputs) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ConnectedUserIntegrationWorkflowEditDialog
                integrationInstance={{connectedUserId: 3, connectionId: 11, id: 7, name: 'instance'} as never}
                integrationInstanceConfiguration={{environmentId: 1, id: 4, name: 'Development'} as never}
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
                onClose={vi.fn()}
                workflow={{...workflow, inputs}}
            />
        </QueryClientProvider>
    );

describe('ConnectedUserIntegrationWorkflowEditDialog', () => {
    beforeEach(() => {
        hoisted.mutate.mockReset();
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

        expect(hoisted.mutate).toHaveBeenCalledWith({
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
});
