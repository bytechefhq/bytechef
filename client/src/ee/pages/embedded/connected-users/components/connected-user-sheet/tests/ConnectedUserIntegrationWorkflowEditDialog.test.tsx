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
                integrationInstance={{connectedUserId: 3, id: 7, name: 'instance'} as never}
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

    it('lists only the inputs a connected user sets', () => {
        renderDialog();

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

    it('explains where internal-only inputs are set when the workflow has no other inputs', () => {
        renderDialog([{internalOnly: true, name: 'contactPageUrl'}] as Workflow['inputs']);

        expect(screen.getByText(/Internal-only inputs are set on the integration configuration/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });
});
