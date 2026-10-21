import {render, screen, stubMutation, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppWorkflowsTab from '../components/AppWorkflowsTab';

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableWorkflowsQuery: vi.fn(() => ({data: {appBindableWorkflows: []}})),
    useAppWorkflowsQuery: vi.fn(() => ({
        data: {
            appWorkflows: [
                {
                    appId: '3',
                    available: true,
                    id: '31',
                    projectDeploymentId: '11',
                    projectDeploymentName: 'Refunds production',
                    workflowKey: 'approve_refund',
                    workflowName: 'Approve refund',
                    workflowUuid: 'uuid-1',
                },
                {
                    appId: '3',
                    available: false,
                    id: '32',
                    projectDeploymentId: '12',
                    projectDeploymentName: 'Legacy deployment',
                    workflowKey: 'legacy_flow',
                    workflowName: null,
                    workflowUuid: 'uuid-2',
                },
            ],
        },
        error: null,
        isLoading: false,
    })),
    useBindAppWorkflowMutation: vi.fn(),
    useUnbindAppWorkflowMutation: vi.fn(),
}));

const {useBindAppWorkflowMutation, useUnbindAppWorkflowMutation} = await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useBindAppWorkflowMutation));

const unbindMutation = stubMutation(vi.mocked(useUnbindAppWorkflowMutation));

describe('AppWorkflowsTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders a row per binding with its key, workflow name and deployment name', () => {
        render(<AppWorkflowsTab app={{id: '3'} as never} canManage />);

        const approveRow = screen.getByRole('row', {name: /approve_refund/});

        expect(within(approveRow).getByText('Approve refund')).toBeInTheDocument();
        expect(within(approveRow).getByText('Refunds production')).toBeInTheDocument();
    });

    it('marks a binding that is not available and shows no workflow name for it', () => {
        render(<AppWorkflowsTab app={{id: '3'} as never} canManage />);

        const legacyRow = screen.getByRole('row', {name: /legacy_flow/});

        expect(within(legacyRow).getByText('Unavailable')).toBeInTheDocument();
        expect(within(legacyRow).getByText('Legacy deployment')).toBeInTheDocument();

        const approveRow = screen.getByRole('row', {name: /approve_refund/});

        expect(within(approveRow).queryByText('Unavailable')).not.toBeInTheDocument();
    });

    it('hides the bind and unbind buttons when the user cannot manage the app', () => {
        render(<AppWorkflowsTab app={{id: '3'} as never} canManage={false} />);

        expect(screen.queryByRole('button', {name: 'Bind Workflow'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Unbind/})).not.toBeInTheDocument();
    });

    it('unbinds a workflow after the confirmation', async () => {
        render(<AppWorkflowsTab app={{id: '3'} as never} canManage />);

        await userEvent.click(screen.getByRole('button', {name: 'Unbind approve_refund'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Unbind'}));

        await waitFor(() => {
            expect(unbindMutation.mutate).toHaveBeenCalledWith({appId: '3', workflowKey: 'approve_refund'});
        });
    });
});
