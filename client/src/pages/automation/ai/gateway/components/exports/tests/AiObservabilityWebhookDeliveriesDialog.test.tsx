import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiObservabilityWebhookDeliveriesDialog from '../AiObservabilityWebhookDeliveriesDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useAiObservabilityWebhookDeliveriesQuery: () => ({
        data: {aiObservabilityWebhookDeliveries: []},
        isLoading: false,
    }),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(
        <AiObservabilityWebhookDeliveriesDialog
            onClose={onClose}
            subscriptionId="1"
            subscriptionName="My Subscription"
        />
    );

    return onClose;
};

describe('AiObservabilityWebhookDeliveriesDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Deliveries for My Subscription'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
