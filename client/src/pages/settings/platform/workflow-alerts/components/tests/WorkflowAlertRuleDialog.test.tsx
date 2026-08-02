import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowAlertRuleDialog from '../WorkflowAlertRuleDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    WorkflowAlertRuleType: {
        ConsecutiveFailures: 'CONSECUTIVE_FAILURES',
        CostThreshold: 'COST_THRESHOLD',
        ErrorCount: 'ERROR_COUNT',
        FailureRate: 'FAILURE_RATE',
        LatencySpike: 'LATENCY_SPIKE',
        LatencyThreshold: 'LATENCY_THRESHOLD',
        NoActivity: 'NO_ACTIVITY',
        UsageThreshold: 'USAGE_THRESHOLD',
    },
    useCreateWorkflowAlertRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateWorkflowAlertRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useWorkspaceNotificationsQuery: () => ({
        data: {
            workspaceNotifications: [{id: '1', name: 'Ops Email', type: 'EMAIL'}],
        },
    }),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<WorkflowAlertRuleDialog onClose={onClose} />);

    return onClose;
};

describe('WorkflowAlertRuleDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Alert Rule'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Rule Type')).toBeInTheDocument();
        expect(screen.getByLabelText('Failures (count)')).toBeInTheDocument();
        expect(screen.getByLabelText('Window (minutes)')).toBeInTheDocument();
        expect(screen.getByLabelText('Cooldown (minutes)')).toBeInTheDocument();
        expect(screen.getByLabelText('Workflow Id (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Ops Email (EMAIL)')).toBeInTheDocument();
    });

    it('groups the notification checkboxes under a legend', () => {
        renderDialog();

        expect(screen.getByRole('group', {name: 'Notifications'})).toBeInTheDocument();
    });

    it('renders the rule type control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Rule Type')).toHaveAttribute('data-slot', 'select-trigger');
    });
});
