import {act, fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PrepaidBilling from '../PrepaidBilling';

const hoisted = vi.hoisted(() => ({
    cancelMutation: {isPending: false, mutate: vi.fn()},
    customerPortalUrl: '',
    reactivateMutation: {isPending: false, mutate: vi.fn()},
    toast: Object.assign(vi.fn(), {error: vi.fn()}),
    useGetCurrentSubscriptionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/billing.queries', () => ({
    useGetCurrentSubscriptionQuery: hoisted.useGetCurrentSubscriptionQuery,
}));

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useCancelSubscriptionMutation: () => hoisted.cancelMutation,
    useReactivateSubscriptionMutation: () => hoisted.reactivateMutation,
}));

vi.mock('../components/BuyJobExecutionsPackageDialog', () => ({
    default: ({onClose, open}: {onClose: () => void; open: boolean}) =>
        open ? (
            <div data-testid="buy-package-dialog">
                <button onClick={onClose}>Mock close package</button>
            </div>
        ) : null,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({billing: {customerPortalUrl: hoisted.customerPortalUrl}}),
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

vi.mock('../components/PrepaidSelectPlanDialog', () => ({
    default: ({
        onClose,
        onUpgradeSuccess,
        open,
    }: {
        onClose: () => void;
        onUpgradeSuccess: (isUpgrade: boolean, newPlanName: string) => void;
        open: boolean;
    }) =>
        open ? (
            <div data-testid="select-plan-dialog">
                <button onClick={() => onUpgradeSuccess(true, 'Growth')}>Mock upgrade</button>

                <button onClick={() => onUpgradeSuccess(false, 'Starter')}>Mock downgrade</button>

                <button onClick={onClose}>Mock close</button>
            </div>
        ) : null,
}));

const DAY_MS = 24 * 60 * 60 * 1000;

const activeSubscription = {
    cancelAtPeriodEnd: false,
    currentPeriodEnd: new Date(2026, 11, 1),
    extraUnitTotal: 1000,
    extraUnitUsed: 100,
    jobsExecuted: 400,
    planName: 'STARTER',
    planUnitTotal: 5000,
    planUnitUsed: 10,
    productUnitLimit: 1000,
    status: 'ACTIVE',
    subscriptionModel: 'PREPAID',
};

const LocationSearch = () => <span data-testid="location-search">{useLocation().search}</span>;

const renderPrepaidBilling = (initialEntry = '/') =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <PrepaidBilling />

            <LocationSearch />
        </MemoryRouter>
    );

const mockSubscription = (subscription: object | undefined, error: Error | null = null) =>
    hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: subscription, error});

const advancePollAttempts = async (attempts: number) => {
    for (let attempt = 0; attempt < attempts; attempt++) {
        await act(async () => {
            await vi.advanceTimersByTimeAsync(3000);
        });
    }
};

describe('PrepaidBilling', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.cancelMutation = {isPending: false, mutate: vi.fn()};
        hoisted.customerPortalUrl = '';
        hoisted.reactivateMutation = {isPending: false, mutate: vi.fn()};
        hoisted.useGetCurrentSubscriptionQuery.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();

        resetAll();
    });

    describe('plan card', () => {
        it('shows the error message when the subscription cannot be loaded', () => {
            mockSubscription(undefined, new Error('failed'));

            renderPrepaidBilling();

            expect(screen.getByText(/Unable to load subscription/)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Change plan'})).not.toBeInTheDocument();
        });

        it('shows the trial with the remaining days and no cancel action', () => {
            mockSubscription({
                ...activeSubscription,
                currentPeriodEnd: new Date(Date.now() + 3 * DAY_MS - 1000),
                planName: 'TRIAL',
            });

            renderPrepaidBilling();

            expect(screen.getByText('Trial')).toBeInTheDocument();
            expect(screen.getByText('3 days')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel plan'})).not.toBeInTheDocument();
        });

        it('shows the trial when there is no subscription', () => {
            mockSubscription(undefined);

            renderPrepaidBilling();

            expect(screen.getByText('Trial')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Change plan'})).toBeInTheDocument();
        });

        it('shows an expired trial', () => {
            mockSubscription({...activeSubscription, planName: 'TRIAL', status: 'CANCELED'});

            renderPrepaidBilling();

            expect(screen.getByText('Your trial has expired.')).toBeInTheDocument();
        });

        it('shows the renewal date of an active plan', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            expect(screen.getByText('STARTER')).toBeInTheDocument();
            expect(screen.getByText('December 1, 2026')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel plan'})).toBeInTheDocument();
        });

        it('falls back to default plan values when the subscription leaves them out', () => {
            mockSubscription({status: 'ACTIVE', subscriptionModel: 'PREPAID'});

            renderPrepaidBilling();

            expect(screen.getByText('Active')).toBeInTheDocument();
            expect(screen.getByText('Job executions used this period')).toBeInTheDocument();
        });

        it('shows the manage billing card when a customer portal is configured', () => {
            hoisted.customerPortalUrl = 'https://billing.test/portal';

            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            expect(screen.getByRole('button', {name: /Manage billing/})).toBeInTheDocument();
        });

        it('hides the manage billing card without a customer portal', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            expect(screen.queryByRole('button', {name: /Manage billing/})).not.toBeInTheDocument();
        });
    });

    describe('cancel and reactivate', () => {
        it('cancels the subscription and starts polling for the cancellation', () => {
            hoisted.cancelMutation.mutate.mockImplementation((_variables, options) => options.onSuccess());

            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

            expect(screen.getByText('Cancel subscription?')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel subscription'}));

            expect(hoisted.cancelMutation.mutate).toHaveBeenCalledTimes(1);
            expect(screen.getByTestId('location-search')).toHaveTextContent('?cancel=pending');
            expect(screen.getByText('Scheduling cancellation, please wait…')).toBeInTheDocument();
        });

        it('keeps the plan when the cancel dialog is dismissed', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));
            fireEvent.click(screen.getByRole('button', {name: 'Keep plan'}));

            expect(screen.queryByText('Cancel subscription?')).not.toBeInTheDocument();
            expect(hoisted.cancelMutation.mutate).not.toHaveBeenCalled();
        });

        it('shows the pending cancel label while the cancellation is in flight', () => {
            hoisted.cancelMutation = {isPending: true, mutate: vi.fn()};

            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

            expect(screen.getByRole('button', {name: 'Cancelling…'})).toBeInTheDocument();
        });

        it('reactivates a subscription cancelled at period end and starts polling', () => {
            hoisted.reactivateMutation.mutate.mockImplementation((_variables, options) => options.onSuccess());

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling();

            expect(screen.queryByRole('button', {name: 'Cancel plan'})).not.toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

            expect(screen.getByText('Reactivate subscription?')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate subscription'}));

            expect(hoisted.reactivateMutation.mutate).toHaveBeenCalledTimes(1);
            expect(screen.getByTestId('location-search')).toHaveTextContent('?reactivate=pending');
        });

        it('keeps the subscription cancelled when the reactivate dialog is dismissed', () => {
            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));
            fireEvent.click(screen.getByRole('button', {name: 'Keep cancelled'}));

            expect(screen.queryByText('Reactivate subscription?')).not.toBeInTheDocument();
            expect(hoisted.reactivateMutation.mutate).not.toHaveBeenCalled();
        });

        it('shows the pending reactivate label while the reactivation is in flight', () => {
            hoisted.reactivateMutation = {isPending: true, mutate: vi.fn()};

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

            expect(screen.getByRole('button', {name: 'Reactivating…'})).toBeInTheDocument();
        });
    });

    describe('plan change', () => {
        it('opens and closes the select plan dialog', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Change plan'}));

            expect(screen.getByTestId('select-plan-dialog')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Mock close'}));

            expect(screen.queryByTestId('select-plan-dialog')).not.toBeInTheDocument();
        });

        it('polls for an upgrade until the new plan is active', () => {
            mockSubscription(activeSubscription);

            const {rerender} = renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Change plan'}));
            fireEvent.click(screen.getByRole('button', {name: 'Mock upgrade'}));

            expect(screen.getByTestId('location-search')).toHaveTextContent('?upgrade=pending');
            expect(screen.getByText('Upgrading your plan, please wait…')).toBeInTheDocument();

            mockSubscription({...activeSubscription, planName: 'GROWTH'});

            rerender(
                <MemoryRouter>
                    <PrepaidBilling />

                    <LocationSearch />
                </MemoryRouter>
            );

            expect(screen.queryByText('Upgrading your plan, please wait…')).not.toBeInTheDocument();
        });

        it('announces a downgrade at the end of the billing period', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Change plan'}));
            fireEvent.click(screen.getByRole('button', {name: 'Mock downgrade'}));

            expect(hoisted.toast).toHaveBeenCalledWith(
                'Your plan will be downgraded at the end of the current billing period.'
            );
            expect(screen.getByTestId('location-search')).toHaveTextContent('');
        });
    });

    describe('polling', () => {
        it('polls after checkout until the paid subscription arrives', () => {
            mockSubscription({...activeSubscription, planName: 'TRIAL'});

            renderPrepaidBilling('/?checkout=success');

            expect(screen.getByText('Activating your subscription, please wait…')).toBeInTheDocument();
            expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenLastCalledWith({refetchInterval: 3000});
        });

        it('clears the checkout parameter once the paid subscription arrives', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?checkout=success');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
            expect(screen.queryByText('Activating your subscription, please wait…')).not.toBeInTheDocument();
        });

        it('stops polling after checkout and asks for a refresh when it takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(undefined);

            renderPrepaidBilling('/?checkout=success');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
            expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenLastCalledWith({refetchInterval: false});
        });

        it('clears the cancel parameter once the cancellation is stored', () => {
            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling('/?cancel=pending');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
        });

        it('asks for a refresh when the cancellation takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?cancel=pending');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });

        it('clears the reactivate parameter once the reactivation is stored', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?reactivate=pending');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
        });

        it('asks for a refresh when the reactivation takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling('/?reactivate=pending');

            expect(screen.getByText('Reactivating your subscription, please wait…')).toBeInTheDocument();

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });

        it('asks for a refresh when the upgrade takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?upgrade=pending');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });
    });

    describe('extra units', () => {
        it('hides the extra units card for a trial', () => {
            mockSubscription({...activeSubscription, planName: 'TRIAL'});

            renderPrepaidBilling();

            expect(screen.queryByRole('button', {name: 'Buy extra units'})).not.toBeInTheDocument();
        });

        it('opens and closes the buy package dialog from an active plan', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            const buyButton = screen.getByRole('button', {name: 'Buy extra units'});

            expect(buyButton).toBeEnabled();

            fireEvent.click(buyButton);

            expect(screen.getByTestId('buy-package-dialog')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Mock close package'}));

            expect(screen.queryByTestId('buy-package-dialog')).not.toBeInTheDocument();
        });

        it('disables the buy button when the subscription is not active', () => {
            mockSubscription({...activeSubscription, status: 'PAST_DUE'});

            renderPrepaidBilling();

            expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeDisabled();
        });

        it('disables the buy button when an active subscription is cancelled at period end', () => {
            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderPrepaidBilling();

            expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeDisabled();
        });

        it('shows the package banner and refetches after package success, then clears it', async () => {
            vi.useFakeTimers();

            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?package=success');

            expect(screen.getByText('Adding job executions, please wait…')).toBeInTheDocument();
            expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenLastCalledWith({refetchInterval: 3000});
            expect(screen.getByTestId('location-search')).toHaveTextContent('?package=success');

            await advancePollAttempts(5);

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
            expect(screen.queryByText('Adding job executions, please wait…')).not.toBeInTheDocument();
        });
    });
});
