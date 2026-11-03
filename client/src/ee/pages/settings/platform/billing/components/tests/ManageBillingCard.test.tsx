import {render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import ManageBillingCard from '../ManageBillingCard';

describe('ManageBillingCard', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('describes the billing portal', () => {
        render(<ManageBillingCard customerPortalUrl="https://billing.test/portal" />);

        expect(screen.getByText('Billing & invoices')).toBeInTheDocument();
        expect(
            screen.getByText('Update your payment method and billing details, and view or download past invoices.')
        ).toBeInTheDocument();
    });

    it('opens the customer portal in a new tab without an opener', async () => {
        const windowOpen = vi.spyOn(window, 'open').mockImplementation(() => null);

        render(<ManageBillingCard customerPortalUrl="https://billing.test/portal" />);

        await userEvent.click(screen.getByRole('button', {name: /Manage billing & invoices/}));

        expect(windowOpen).toHaveBeenCalledWith('https://billing.test/portal', '_blank', 'noopener,noreferrer');
    });
});
