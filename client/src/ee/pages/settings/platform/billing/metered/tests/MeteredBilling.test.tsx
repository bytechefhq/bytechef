import {act, fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import MeteredBilling from '../MeteredBilling';

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

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({billing: {customerPortalUrl: hoisted.customerPortalUrl}}),
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

vi.mock('../components/SelectPlanDialog', () => ({
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
    jobsExecuted: 400,
    planName: 'STARTER',
    productUnitLimit: 1000,
    status: 'ACTIVE',
    subscriptionModel: 'METERED',
};

const LocationSearch = () => <span data-testid="location-search">{useLocation().search}</span>;

const renderMeteredBilling = (initialEntry = '/') =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <MeteredBilling />

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

describe('MeteredBilling', () => {
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

            renderMeteredBilling();

            expect(screen.getByText(/Unable to load subscription/)).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Change plan'})).not.toBeInTheDocument();
        });

        it('shows the trial with the remaining days and no cancel action', () => {
            mockSubscription({
                ...activeSubscription,
                currentPeriodEnd: new Date(Date.now() + 3 * DAY_MS - 1000),
                planName: 'TRIAL',
            });

            renderMeteredBilling();

            expect(screen.getByText('Trial')).toBeInTheDocument();
            expect(screen.getByText('3 days')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel plan'})).not.toBeInTheDocument();
        });

        it('shows the trial when there is no subscription', () => {
            mockSubscription(undefined);

            renderMeteredBilling();

            expect(screen.getByText('Trial')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Change plan'})).toBeInTheDocument();
        });

        it('shows an expired trial', () => {
            mockSubscription({...activeSubscription, planName: 'TRIAL', status: 'CANCELED'});

            renderMeteredBilling();

            expect(screen.getByText('Your trial has expired.')).toBeInTheDocument();
        });

        it('shows the renewal date of an active plan', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling();

            expect(screen.getByText('STARTER')).toBeInTheDocument();
            expect(screen.getByText('December 1, 2026')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel plan'})).toBeInTheDocument();
        });

        it('falls back to default plan values when the subscription leaves them out', () => {
            mockSubscription({status: 'ACTIVE', subscriptionModel: 'METERED'});

            renderMeteredBilling();

            expect(screen.getByText('Active')).toBeInTheDocument();
            expect(screen.getByText('Job executions used this period')).toBeInTheDocument();
        });

        it('shows the manage billing card when a customer portal is configured', () => {
            hoisted.customerPortalUrl = 'https://billing.test/portal';

            mockSubscription(activeSubscription);

            renderMeteredBilling();

            expect(screen.getByRole('button', {name: /Manage billing/})).toBeInTheDocument();
        });

        it('hides the manage billing card without a customer portal', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling();

            expect(screen.queryByRole('button', {name: /Manage billing/})).not.toBeInTheDocument();
        });
    });

    describe('cancel and reactivate', () => {
        it('cancels the subscription and starts polling for the cancellation', () => {
            hoisted.cancelMutation.mutate.mockImplementation((_variables, options) => options.onSuccess());

            mockSubscription(activeSubscription);

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

            expect(screen.getByText('Cancel subscription?')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel subscription'}));

            expect(hoisted.cancelMutation.mutate).toHaveBeenCalledTimes(1);
            expect(screen.getByTestId('location-search')).toHaveTextContent('?cancel=pending');
            expect(screen.getByText('Scheduling cancellation, please wait…')).toBeInTheDocument();
        });

        it('keeps the plan when the cancel dialog is dismissed', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));
            fireEvent.click(screen.getByRole('button', {name: 'Keep plan'}));

            expect(screen.queryByText('Cancel subscription?')).not.toBeInTheDocument();
            expect(hoisted.cancelMutation.mutate).not.toHaveBeenCalled();
        });

        it('shows the pending cancel label while the cancellation is in flight', () => {
            hoisted.cancelMutation = {isPending: true, mutate: vi.fn()};

            mockSubscription(activeSubscription);

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

            expect(screen.getByRole('button', {name: 'Cancelling…'})).toBeInTheDocument();
        });

        it('reactivates a subscription cancelled at period end and starts polling', () => {
            hoisted.reactivateMutation.mutate.mockImplementation((_variables, options) => options.onSuccess());

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderMeteredBilling();

            expect(screen.queryByRole('button', {name: 'Cancel plan'})).not.toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

            expect(screen.getByText('Reactivate subscription?')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate subscription'}));

            expect(hoisted.reactivateMutation.mutate).toHaveBeenCalledTimes(1);
            expect(screen.getByTestId('location-search')).toHaveTextContent('?reactivate=pending');
        });

        it('keeps the subscription cancelled when the reactivate dialog is dismissed', () => {
            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));
            fireEvent.click(screen.getByRole('button', {name: 'Keep cancelled'}));

            expect(screen.queryByText('Reactivate subscription?')).not.toBeInTheDocument();
            expect(hoisted.reactivateMutation.mutate).not.toHaveBeenCalled();
        });

        it('shows the pending reactivate label while the reactivation is in flight', () => {
            hoisted.reactivateMutation = {isPending: true, mutate: vi.fn()};

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

            expect(screen.getByRole('button', {name: 'Reactivating…'})).toBeInTheDocument();
        });
    });

    describe('plan change', () => {
        it('opens and closes the select plan dialog', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Change plan'}));

            expect(screen.getByTestId('select-plan-dialog')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Mock close'}));

            expect(screen.queryByTestId('select-plan-dialog')).not.toBeInTheDocument();
        });

        it('polls for an upgrade until the new plan is active', () => {
            mockSubscription(activeSubscription);

            const {rerender} = renderMeteredBilling();

            fireEvent.click(screen.getByRole('button', {name: 'Change plan'}));
            fireEvent.click(screen.getByRole('button', {name: 'Mock upgrade'}));

            expect(screen.getByTestId('location-search')).toHaveTextContent('?upgrade=pending');
            expect(screen.getByText('Upgrading your plan, please wait…')).toBeInTheDocument();

            mockSubscription({...activeSubscription, planName: 'GROWTH'});

            rerender(
                <MemoryRouter>
                    <MeteredBilling />

                    <LocationSearch />
                </MemoryRouter>
            );

            expect(screen.queryByText('Upgrading your plan, please wait…')).not.toBeInTheDocument();
        });

        it('announces a downgrade at the end of the billing period', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling();

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

            renderMeteredBilling('/?checkout=success');

            expect(screen.getByText('Activating your subscription, please wait…')).toBeInTheDocument();
            expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenLastCalledWith({refetchInterval: 3000});
        });

        it('clears the checkout parameter once the paid subscription arrives', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling('/?checkout=success');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
            expect(screen.queryByText('Activating your subscription, please wait…')).not.toBeInTheDocument();
        });

        it('stops polling after checkout and asks for a refresh when it takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(undefined);

            renderMeteredBilling('/?checkout=success');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
            expect(hoisted.useGetCurrentSubscriptionQuery).toHaveBeenLastCalledWith({refetchInterval: false});
        });

        it('clears the cancel parameter once the cancellation is stored', () => {
            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderMeteredBilling('/?cancel=pending');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
        });

        it('asks for a refresh when the cancellation takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(activeSubscription);

            renderMeteredBilling('/?cancel=pending');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });

        it('clears the reactivate parameter once the reactivation is stored', () => {
            mockSubscription(activeSubscription);

            renderMeteredBilling('/?reactivate=pending');

            expect(screen.getByTestId('location-search')).toHaveTextContent('');
        });

        it('asks for a refresh when the reactivation takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription({...activeSubscription, cancelAtPeriodEnd: true});

            renderMeteredBilling('/?reactivate=pending');

            expect(screen.getByText('Reactivating your subscription, please wait…')).toBeInTheDocument();

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });

        it('asks for a refresh when the upgrade takes too long', async () => {
            vi.useFakeTimers();

            mockSubscription(activeSubscription);

            renderMeteredBilling('/?upgrade=pending');

            await advancePollAttempts(5);

            expect(screen.getByText("It's taking longer than expected. Please refresh the page.")).toBeInTheDocument();
        });
    });
});
