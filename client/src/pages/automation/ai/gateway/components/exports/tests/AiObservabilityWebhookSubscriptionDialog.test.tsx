import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiObservabilityWebhookSubscriptionDialog from '../AiObservabilityWebhookSubscriptionDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiObservabilityWebhookSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiObservabilityWebhookSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiObservabilityWebhookSubscriptionDialog onClose={onClose} />);

    return onClose;
};

describe('AiObservabilityWebhookSubscriptionDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Webhook Subscription'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('URL')).toBeInTheDocument();
        expect(screen.getByLabelText('Secret (HMAC-SHA256)')).toBeInTheDocument();
        expect(screen.getByLabelText('alert.triggered')).toBeInTheDocument();
        expect(screen.getByLabelText('budget.exceeded')).toBeInTheDocument();
        expect(screen.getByLabelText('trace.completed')).toBeInTheDocument();
        expect(screen.getByLabelText('Enabled')).toBeInTheDocument();
        expect(screen.getByRole('group', {name: 'Events'})).toBeInTheDocument();
    });
});
