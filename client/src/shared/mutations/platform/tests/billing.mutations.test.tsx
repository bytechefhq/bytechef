import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {
    useCancelSubscriptionMutation,
    useCreateJobExecutionsPackageCheckoutSessionMutation,
    useReactivateSubscriptionMutation,
    useUpgradeSubscriptionMutation,
} from '../billing.mutations';

const hoisted = vi.hoisted(() => ({
    cancelSubscription: vi.fn(),
    createJobExecutionsPackageCheckoutSession: vi.fn(),
    reactivateSubscription: vi.fn(),
    upgradeSubscription: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/billing', () => ({
    BillingApi: class {
        cancelSubscription = hoisted.cancelSubscription;
        createJobExecutionsPackageCheckoutSession = hoisted.createJobExecutionsPackageCheckoutSession;
        reactivateSubscription = hoisted.reactivateSubscription;
        upgradeSubscription = hoisted.upgradeSubscription;
    },
}));

describe('billing mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('cancels the subscription through the billing API', async () => {
        hoisted.cancelSubscription.mockResolvedValue(undefined);

        const {result} = renderHook(() => useCancelSubscriptionMutation(), {wrapper: createTestQueryClientWrapper()});

        result.current.mutate();

        await waitFor(() => expect(hoisted.cancelSubscription).toHaveBeenCalledTimes(1));
    });

    it('reactivates the subscription through the billing API', async () => {
        hoisted.reactivateSubscription.mockResolvedValue(undefined);

        const {result} = renderHook(() => useReactivateSubscriptionMutation(), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate();

        await waitFor(() => expect(hoisted.reactivateSubscription).toHaveBeenCalledTimes(1));
    });

    it('sends the new plan name when changing the plan', async () => {
        hoisted.upgradeSubscription.mockResolvedValue(undefined);

        const {result} = renderHook(() => useUpgradeSubscriptionMutation(), {wrapper: createTestQueryClientWrapper()});

        result.current.mutate('GROWTH');

        await waitFor(() =>
            expect(hoisted.upgradeSubscription).toHaveBeenCalledWith({checkoutSessionRequest: {planName: 'GROWTH'}})
        );
    });

    it('sends the package quantity when starting a package checkout', async () => {
        hoisted.createJobExecutionsPackageCheckoutSession.mockResolvedValue({checkoutUrl: 'https://stripe.test'});

        const onSuccess = vi.fn();

        const {result} = renderHook(() => useCreateJobExecutionsPackageCheckoutSessionMutation(), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate(3, {onSuccess});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(hoisted.createJobExecutionsPackageCheckoutSession).toHaveBeenCalledWith({
            jobExecutionsPackageCheckoutRequest: {quantity: 3},
        });
        expect(onSuccess.mock.calls[0][0]).toEqual({checkoutUrl: 'https://stripe.test'});
    });
});
