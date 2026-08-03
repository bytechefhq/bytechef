import {fireEvent, render, screen, stubMutation} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

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
    useCreateWorkflowAlertRuleMutation: vi.fn(),
    useUpdateWorkflowAlertRuleMutation: vi.fn(),
    useWorkspaceNotificationsQuery: () => ({
        data: {
            workspaceNotifications: [{id: '1', name: 'Ops Email', type: 'EMAIL'}],
        },
    }),
}));

const {useCreateWorkflowAlertRuleMutation, useUpdateWorkflowAlertRuleMutation} =
    await import('@/shared/middleware/graphql');

const createRuleMutation = stubMutation(vi.mocked(useCreateWorkflowAlertRuleMutation));
const updateRuleMutation = stubMutation(vi.mocked(useUpdateWorkflowAlertRuleMutation));

beforeEach(() => {
    createRuleMutation.mutate.mockClear();
    updateRuleMutation.mutate.mockClear();
});

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

describe('WorkflowAlertRuleDialog submit', () => {
    it('sends the form input to the create mutation and closes the dialog once it succeeds', () => {
        const onClose = renderDialog();

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Consecutive failures on order sync'}});

        fireEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(createRuleMutation.mutate).toHaveBeenCalledWith({
            input: {
                cooldownMinutes: 60,
                enabled: true,
                name: 'Consecutive failures on order sync',
                notificationIds: [],
                ruleType: 'CONSECUTIVE_FAILURES',
                threshold: 3,
                windowMinutes: 60,
                workflowId: null,
            },
            workspaceId: '1',
        });

        expect(onClose).not.toHaveBeenCalled();

        createRuleMutation.triggerOnSuccess(undefined, undefined);

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
