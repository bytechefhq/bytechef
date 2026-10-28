import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Billing from '../Billing';

const hoisted = vi.hoisted(() => ({
    useGetCurrentSubscriptionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/billing.queries', () => ({
    useGetCurrentSubscriptionQuery: hoisted.useGetCurrentSubscriptionQuery,
}));

vi.mock('../metered/MeteredBilling', () => ({
    default: () => <div data-testid="metered-billing" />,
}));

vi.mock('../prepaid/PrepaidBilling', () => ({
    default: () => <div data-testid="prepaid-billing" />,
}));

describe('Billing', () => {
    beforeEach(() => {
        hoisted.useGetCurrentSubscriptionQuery.mockReset();
    });

    it('renders metered page for METERED subscription', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({
            data: {subscriptionModel: 'METERED'},
            error: null,
            isPending: false,
        });

        render(<Billing />);

        expect(screen.getByTestId('metered-billing')).toBeInTheDocument();
        expect(screen.queryByTestId('prepaid-billing')).not.toBeInTheDocument();
    });

    it('renders prepaid page for PREPAID subscription', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({
            data: {subscriptionModel: 'PREPAID'},
            error: null,
            isPending: false,
        });

        render(<Billing />);

        expect(screen.getByTestId('prepaid-billing')).toBeInTheDocument();
        expect(screen.queryByTestId('metered-billing')).not.toBeInTheDocument();
    });

    it('renders prepaid page when there is no subscription', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: null, error: null, isPending: false});

        render(<Billing />);

        expect(screen.getByTestId('prepaid-billing')).toBeInTheDocument();
        expect(screen.queryByTestId('metered-billing')).not.toBeInTheDocument();
    });

    it('renders nothing while loading', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: undefined, error: null, isPending: true});

        render(<Billing />);

        expect(screen.queryByTestId('metered-billing')).not.toBeInTheDocument();
        expect(screen.queryByTestId('prepaid-billing')).not.toBeInTheDocument();
        expect(screen.queryByText(/Unable to load subscription/)).not.toBeInTheDocument();
    });

    it('renders the error banner and neither page on error', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({
            data: undefined,
            error: new Error('boom'),
            isPending: false,
        });

        render(<Billing />);

        expect(screen.getByText(/Unable to load subscription/)).toBeInTheDocument();
        expect(screen.queryByTestId('metered-billing')).not.toBeInTheDocument();
        expect(screen.queryByTestId('prepaid-billing')).not.toBeInTheDocument();
    });
});
