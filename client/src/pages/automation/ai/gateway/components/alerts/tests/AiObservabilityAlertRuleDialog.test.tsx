import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiObservabilityAlertRuleDialog from '../AiObservabilityAlertRuleDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    AiObservabilityAlertCondition: {Equals: 'EQUALS', GreaterThan: 'GREATER_THAN', LessThan: 'LESS_THAN'},
    AiObservabilityAlertMetric: {
        Cost: 'COST',
        ErrorRate: 'ERROR_RATE',
        LatencyP95: 'LATENCY_P95',
        RequestVolume: 'REQUEST_VOLUME',
        TokenUsage: 'TOKEN_USAGE',
    },
    useCreateAiObservabilityAlertRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useTestAiObservabilityAlertRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiObservabilityAlertRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useWorkspaceNotificationsQuery: () => ({
        data: {
            workspaceNotifications: [{id: '1', name: 'Ops Email', type: 'EMAIL'}],
        },
    }),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiObservabilityAlertRuleDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiObservabilityAlertRuleDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Alert Rule'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Metric')).toBeInTheDocument();
        expect(screen.getByLabelText('Condition')).toBeInTheDocument();
        expect(screen.getByLabelText('Threshold')).toBeInTheDocument();
        expect(screen.getByLabelText('Window (minutes)')).toBeInTheDocument();
        expect(screen.getByLabelText('Cooldown (minutes)')).toBeInTheDocument();
        expect(screen.getByLabelText('Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Ops Email (EMAIL)')).toBeInTheDocument();
    });

    it('groups the notification checkboxes under a legend', () => {
        renderDialog();

        expect(screen.getByRole('group', {name: 'Notifications'})).toBeInTheDocument();
    });

    it('renders the metric control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Metric')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('renders the condition control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Condition')).toHaveAttribute('data-slot', 'select-trigger');
    });
});
