import {act, render, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PrepaidBilling from '../PrepaidBilling';

const hoisted = vi.hoisted(() => ({
    searchParams: new URLSearchParams(),
    setSearchParams: vi.fn(),
    subscription: {current: undefined as unknown},
    useGetCurrentSubscriptionQuery: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useSearchParams: () => [hoisted.searchParams, hoisted.setSearchParams],
}));

vi.mock('@/shared/queries/platform/billing.queries', () => ({
    useGetCurrentSubscriptionQuery: hoisted.useGetCurrentSubscriptionQuery,
}));

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useCancelSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
    useCreateJobExecutionsPackageCheckoutSessionMutation: () => ({isPending: false, mutate: vi.fn()}),
    useReactivateSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpgradeSubscriptionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({billing: {customerPortalUrl: ''}}),
}));

vi.mock('../components/PrepaidSelectPlanDialog', () => ({default: () => null}));
vi.mock('../components/BuyJobExecutionsPackageDialog', () => ({default: () => null}));

const activeSubscription = {
    cancelAtPeriodEnd: false,
    extraUnitTotal: 1000,
    extraUnitUsed: 100,
    planName: 'STARTER',
    planUnitTotal: 5000,
    planUnitUsed: 10,
    status: 'ACTIVE',
    subscriptionModel: 'PREPAID',
};

describe('PrepaidBilling', () => {
    beforeEach(() => {
        hoisted.searchParams = new URLSearchParams();
        hoisted.setSearchParams.mockReset();
        hoisted.useGetCurrentSubscriptionQuery.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('hides extra units card for trial', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({
            data: {...activeSubscription, planName: 'TRIAL'},
            error: null,
        });

        render(<PrepaidBilling />);

        expect(screen.queryByRole('button', {name: 'Buy extra units'})).not.toBeInTheDocument();
    });

    it('shows extra units card for an active plan', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: activeSubscription, error: null});

        render(<PrepaidBilling />);

        expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeEnabled();
    });

    it('disables the buy button when the subscription is not active', () => {
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({
            data: {...activeSubscription, status: 'PAST_DUE'},
            error: null,
        });

        render(<PrepaidBilling />);

        expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeDisabled();
    });

    it('shows the package banner and refetches after package success, then clears it', async () => {
        vi.useFakeTimers();

        hoisted.searchParams = new URLSearchParams('package=success');
        hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: activeSubscription, error: null});

        render(<PrepaidBilling />);

        expect(screen.getByText('Adding job executions, please wait…')).toBeInTheDocument();
        expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenCalledWith({refetchInterval: 3000});
        expect(hoisted.setSearchParams).not.toHaveBeenCalled();

        for (let attempt = 0; attempt < 5; attempt++) {
            await act(async () => {
                await vi.advanceTimersByTimeAsync(3000);
            });
        }

        expect(hoisted.setSearchParams).toHaveBeenCalledWith({}, {replace: true});
    });
});
