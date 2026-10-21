import Button from '@/components/Button/Button';
import {
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppWorkflowBindingDialog from '../components/AppWorkflowBindingDialog';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableWorkflowsQuery: vi.fn(() => ({
        data: {
            appBindableWorkflows: [
                {
                    description: null,
                    projectDeploymentId: '11',
                    projectDeploymentName: 'Refunds production',
                    projectName: 'Refunds',
                    workflowName: 'Approve refund',
                    workflowUuid: 'uuid-1',
                },
            ],
        },
    })),
    useBindAppWorkflowMutation: vi.fn(),
}));

const {useBindAppWorkflowMutation} = await import('@/shared/middleware/graphql');

const bindMutation = stubMutation(vi.mocked(useBindAppWorkflowMutation));

describe('AppWorkflowBindingDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();
        mockScrollIntoView();
    });

    it('rejects an invalid workflow key', async () => {
        render(<AppWorkflowBindingDialog app={{id: '3'} as never} triggerNode={<Button label="Bind Workflow" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Bind Workflow'}));
        await userEvent.type(screen.getByLabelText('Workflow key'), 'Approve Refund');
        await userEvent.click(screen.getByRole('button', {name: 'Bind'}));

        expect(await screen.findByText(/Workflow key must start with a lowercase letter/)).toBeInTheDocument();
        expect(bindMutation.mutate).not.toHaveBeenCalled();
    });

    it('binds the selected deployed workflow under the typed key', async () => {
        render(<AppWorkflowBindingDialog app={{id: '3'} as never} triggerNode={<Button label="Bind Workflow" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Bind Workflow'}));
        await userEvent.type(screen.getByLabelText('Workflow key'), 'approve_refund');
        await userEvent.click(screen.getByRole('combobox', {name: 'Workflow'}));
        await userEvent.click(await screen.findByText('Approve refund — Refunds (Refunds production)'));
        await userEvent.click(screen.getByRole('button', {name: 'Bind'}));

        await waitFor(() => {
            expect(bindMutation.mutate).toHaveBeenCalledWith({
                appId: '3',
                input: {projectDeploymentId: '11', workflowKey: 'approve_refund', workflowUuid: 'uuid-1'},
            });
        });
    });
});
