import {fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PrepaidBilling from '../PrepaidBilling';

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
            {[...planUsageFeatures.starter, ...planUsageFeatures.growth].map((feature, index) => (
                <li key={`${index}-${feature}`}>{feature}</li>
            ))}
        </ul>
    ),
}));

vi.mock('../components/BuyJobExecutionsPackageDialog', () => ({
    default: ({onClose, open}: {onClose: () => void; open: boolean}) =>
        open ? (
            <div data-testid="buy-package-dialog">
                <button onClick={onClose}>Mock close package</button>
            </div>
        ) : null,
}));

const activeSubscription = {
    cancelAtPeriodEnd: false,
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

const renderPrepaidBilling = (initialEntry = '/') =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <PrepaidBilling />
        </MemoryRouter>
    );

const mockSubscription = (subscription: object | undefined) =>
    hoisted.useGetCurrentSubscriptionQuery.mockReturnValue({data: subscription, error: null});

describe('PrepaidBilling', () => {
    beforeEach(() => {
        hoisted.useGetCurrentSubscriptionQuery.mockReset();
    });

    afterEach(() => {
        resetAll();
    });

    describe('plan card', () => {
        it('shows the plan units used and included', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            expect(screen.getByText('Used 10')).toBeInTheDocument();
            expect(screen.getByText('Included 5,000')).toBeInTheDocument();
        });

        it('falls back to no plan units when the subscription leaves them out', () => {
            mockSubscription({status: 'ACTIVE', subscriptionModel: 'PREPAID'});

            renderPrepaidBilling();

            expect(screen.getByText('Used 0')).toBeInTheDocument();
            expect(screen.getByText('Included 0')).toBeInTheDocument();
        });

        it('offers the prepaid plans', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling();

            const planUsageFeatures = screen.getByTestId('plan-usage-features');

            expect(planUsageFeatures).toHaveTextContent('Price for 5000 workflow executions per month');
            expect(planUsageFeatures).toHaveTextContent('Price for 15000 workflow executions per month');
            expect(screen.getAllByText('+$5 per 1000 additional workflow executions')).toHaveLength(2);
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

        it('falls back to no extra units when the subscription leaves them out', () => {
            mockSubscription({planName: 'STARTER', status: 'ACTIVE', subscriptionModel: 'PREPAID'});

            renderPrepaidBilling();

            expect(screen.getByRole('button', {name: 'Buy extra units'})).toBeEnabled();
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

        it('shows the package banner after a package purchase', () => {
            mockSubscription(activeSubscription);

            renderPrepaidBilling('/?package=success');

            expect(screen.getByText('Adding job executions, please wait…')).toBeInTheDocument();
        });
    });
});
