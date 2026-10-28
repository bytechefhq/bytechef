import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import BuyJobExecutionsPackageDialog from '../BuyJobExecutionsPackageDialog';

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useCreateJobExecutionsPackageCheckoutSessionMutation: () => ({isPending: false, mutate: hoisted.mutate}),
}));

describe('BuyJobExecutionsPackageDialog', () => {
    const originalLocation = window.location;

    beforeEach(() => {
        windowResizeObserver();

        Object.defineProperty(window, 'location', {configurable: true, value: {href: ''}, writable: true});
    });

    afterEach(() => {
        Object.defineProperty(window, 'location', {configurable: true, value: originalLocation, writable: true});

        resetAll();
        vi.clearAllMocks();
    });

    it('shows totals for quantity 3', async () => {
        render(<BuyJobExecutionsPackageDialog onClose={vi.fn()} open />);

        const input = screen.getByLabelText('Quantity');

        await userEvent.clear(input);
        await userEvent.type(input, '3');

        expect(screen.getByText('3,000 job executions for $15')).toBeInTheDocument();
    });

    it('calls mutation with quantity and redirects', async () => {
        hoisted.mutate.mockImplementation((quantity, options) =>
            options.onSuccess({checkoutUrl: 'https://stripe.test/pay'})
        );

        render(<BuyJobExecutionsPackageDialog onClose={vi.fn()} open />);

        const input = screen.getByLabelText('Quantity');

        await userEvent.clear(input);
        await userEvent.type(input, '2');
        await userEvent.click(screen.getByRole('button', {name: 'Continue to checkout'}));

        await waitFor(() => expect(window.location.href).toBe('https://stripe.test/pay'));

        expect(hoisted.mutate).toHaveBeenCalledWith(2, expect.anything());
    });

    it('rejects quantity 0 and 101', async () => {
        render(<BuyJobExecutionsPackageDialog onClose={vi.fn()} open />);

        const input = screen.getByLabelText('Quantity');
        const confirm = screen.getByRole('button', {name: 'Continue to checkout'});

        await userEvent.clear(input);
        await userEvent.type(input, '0');

        expect(confirm).toBeDisabled();

        await userEvent.clear(input);
        await userEvent.type(input, '101');

        expect(confirm).toBeDisabled();
    });
});
