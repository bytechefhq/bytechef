import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import MeteredBilling from '../MeteredBilling';

const hoisted = vi.hoisted(() => ({
    useGetCurrentSubscriptionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/billing.queries', () => ({
    useGetCurrentSubscriptionQuery: hoisted.useGetCurrentSubscriptionQuery,
}));

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useCancelSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
    useReactivateSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({billing: {customerPortalUrl: ''}}),
}));

vi.mock('../../components/SelectPlanDialog', () => ({
    default: ({planUsageFeatures}: {planUsageFeatures: {growth: string[]; starter: string[]}}) => (
        <ul data-testid="plan-usage-features">
            {[...planUsageFeatures.starter, ...planUsageFeatures.growth].map((feature) => (
                <li key={feature}>{feature}</li>
            ))}
        </ul>
    ),
}));

const activeSubscription = {
    cancelAtPeriodEnd: false,
    jobsExecuted: 1500,
    planName: 'STARTER',
    productUnitLimit: 1000,
    status: 'ACTIVE',
    subscriptionModel: 'METERED',
};

const renderMeteredBilling = () =>
    render(
        <MemoryRouter>
            <MeteredBilling />
        </MemoryRouter>
    );

describe('MeteredBilling', () => {
    beforeEach(() => {
        hoisted.useGetCurrentSubscriptionQuery.mockReset();
    });

    afterEach(() => {
        resetAll();
    });

    it('shows metered usage with the overage billed', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: activeSubscription, error: null});

        renderMeteredBilling();

        expect(screen.getByText('1,000 flat rate')).toBeInTheDocument();
        expect(screen.getByText('500 usage billed')).toBeInTheDocument();
    });

    it('offers the metered plans', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: activeSubscription, error: null});

        renderMeteredBilling();

        expect(screen.getByTestId('plan-usage-features')).toHaveTextContent(
            '1000 job executions / month, then $1/1000 job executions'
        );
    });

    it('ignores the package purchase parameter', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: activeSubscription, error: null});

        render(
            <MemoryRouter initialEntries={['/?package=success']}>
                <MeteredBilling />
            </MemoryRouter>
        );

        expect(screen.queryByText('Adding job executions, please wait…')).not.toBeInTheDocument();
    });
});
