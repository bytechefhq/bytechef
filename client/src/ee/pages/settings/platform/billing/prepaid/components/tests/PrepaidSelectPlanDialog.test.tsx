import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PrepaidSelectPlanDialog from '../PrepaidSelectPlanDialog';

const hoisted = vi.hoisted(() => ({
    createCheckoutSession: vi.fn(),
    toast: Object.assign(vi.fn(), {error: vi.fn()}),
    upgradeSubscription: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useUpgradeSubscriptionMutation: () => ({isPending: false, mutate: hoisted.upgradeSubscription, reset: vi.fn()}),
}));

vi.mock('@/shared/middleware/platform/billing', () => ({
    BillingApi: class {
        createCheckoutSession = hoisted.createCheckoutSession;
    },
    CheckoutSessionRequestPlanNameEnum: {Enterprise: 'ENTERPRISE', Growth: 'GROWTH', Starter: 'STARTER', Team: 'TEAM'},
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

vi.mock('../PrepaidPlanTierCard', () => ({
    default: ({
        ctaLabel,
        features,
        name,
        onSelect,
    }: {
        ctaLabel: string;
        features: string[];
        name: string;
        onSelect: () => void;
    }) => (
        <div data-testid="plan-tier-card">
            <button onClick={onSelect}>{`Select ${name}`}</button>

            <span>{`${name} ${ctaLabel}`}</span>

            <ul>
                {features.map((feature) => (
                    <li key={feature}>{feature}</li>
                ))}
            </ul>
        </div>
    ),
}));

const onClose = vi.fn();
const onUpgradeSuccess = vi.fn();

const defaultProps = {hasActiveSubscription: false, onClose, onUpgradeSuccess, open: true};

describe('PrepaidSelectPlanDialog', () => {
    const originalLocation = window.location;

    beforeEach(() => {
        windowResizeObserver();
        vi.spyOn(window, 'open').mockImplementation(() => null);

        Object.defineProperty(window, 'location', {configurable: true, value: {href: ''}, writable: true});
    });

    afterEach(() => {
        Object.defineProperty(window, 'location', {configurable: true, value: originalLocation, writable: true});

        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the title and description', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getByRole('heading', {name: 'Select a plan'})).toBeInTheDocument();
            expect(screen.getByText('You can upgrade, downgrade, or cancel at any time.')).toBeInTheDocument();
        });

        it('should render the plan cards', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getAllByTestId('plan-tier-card')).toHaveLength(3);
        });

        it('should show prepaid starter and growth copy', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getByText('Price for 5000 workflow executions per month')).toBeInTheDocument();
            expect(screen.getByText('Price for 15000 workflow executions per month')).toBeInTheDocument();
            expect(screen.getAllByText('+$5 per 1000 additional workflow executions')).toHaveLength(2);
        });

        it('should not show metered overage copy', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.queryByText(/then \$1\/1000 job executions/)).not.toBeInTheDocument();
        });

        it('should not render when closed', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} open={false} />);

            expect(screen.queryByRole('heading', {name: 'Select a plan'})).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        // The compare-plans action lives in the header's endContent slot, beside
        // the close button the family provides.
        it('should render the compare plans action and the close control', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Compare plans'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should open the pricing page from compare plans', async () => {
            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Compare plans'}));

            expect(window.open).toHaveBeenCalledWith('https://bytechef.io/pricing', '_blank');
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should open the contact page for the enterprise plan', async () => {
            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Select Enterprise'}));

            expect(window.open).toHaveBeenCalledWith('https://bytechef.io/contact', '_blank');
            expect(hoisted.createCheckoutSession).not.toHaveBeenCalled();
        });
    });

    describe('checkout without an active subscription', () => {
        it('should redirect to the checkout page', async () => {
            hoisted.createCheckoutSession.mockResolvedValue({checkoutUrl: 'https://stripe.test/checkout'});

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));

            await waitFor(() => expect(window.location.href).toBe('https://stripe.test/checkout'));

            expect(hoisted.createCheckoutSession).toHaveBeenCalledWith({
                checkoutSessionRequest: {planName: 'STARTER'},
            });
            expect(screen.getByText('Starter Loading…')).toBeInTheDocument();
        });

        it('should show an error when checkout returns no url', async () => {
            hoisted.createCheckoutSession.mockResolvedValue({});

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));

            await waitFor(() =>
                expect(hoisted.toast.error).toHaveBeenCalledWith('Failed to start checkout. Please try again.')
            );

            expect(screen.getByText('Starter Get Started')).toBeInTheDocument();
        });

        it('should show an error when checkout fails', async () => {
            hoisted.createCheckoutSession.mockRejectedValue(new Error('failed'));

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));

            await waitFor(() =>
                expect(hoisted.toast.error).toHaveBeenCalledWith('Failed to start checkout. Please try again.')
            );

            expect(screen.getByText('Starter Get Started')).toBeInTheDocument();
        });
    });

    describe('plan change with an active subscription', () => {
        it('should confirm and apply an upgrade', async () => {
            hoisted.upgradeSubscription.mockImplementation((_planName, options) => options.onSuccess());

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} currentPlanName="Starter" hasActiveSubscription />);

            await user.click(screen.getByRole('button', {name: 'Select Growth'}));

            expect(screen.getByText('Upgrade to Growth?')).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Upgrade now'}));

            expect(hoisted.upgradeSubscription).toHaveBeenCalledWith('GROWTH', expect.anything());
            expect(onUpgradeSuccess).toHaveBeenCalledWith(true, 'Growth');
            expect(onClose).toHaveBeenCalled();
        });

        it('should confirm and apply a downgrade', async () => {
            hoisted.upgradeSubscription.mockImplementation((_planName, options) => options.onSuccess());

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} currentPlanName="Growth" hasActiveSubscription />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));

            expect(screen.getByText('Downgrade to Starter?')).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Confirm downgrade'}));

            expect(hoisted.upgradeSubscription).toHaveBeenCalledWith('STARTER', expect.anything());
            expect(onUpgradeSuccess).toHaveBeenCalledWith(false, 'Starter');
            expect(hoisted.createCheckoutSession).not.toHaveBeenCalled();
        });

        it('should show an error when the plan change fails', async () => {
            hoisted.upgradeSubscription.mockImplementation((_planName, options) => options.onError());

            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} currentPlanName="Growth" hasActiveSubscription />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));
            await user.click(screen.getByRole('button', {name: 'Confirm downgrade'}));

            expect(hoisted.toast.error).toHaveBeenCalledWith('Failed to change plan. Please try again.');
            expect(onUpgradeSuccess).not.toHaveBeenCalled();
            expect(screen.queryByText('Downgrade to Starter?')).not.toBeInTheDocument();
        });

        it('should keep the current plan when the upgrade confirmation is dismissed', async () => {
            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} currentPlanName="Starter" hasActiveSubscription />);

            await user.click(screen.getByRole('button', {name: 'Select Growth'}));
            await user.click(screen.getByRole('button', {name: 'Keep current plan'}));

            expect(screen.queryByText('Upgrade to Growth?')).not.toBeInTheDocument();
            expect(hoisted.upgradeSubscription).not.toHaveBeenCalled();
        });

        it('should keep the current plan when the downgrade confirmation is dismissed', async () => {
            const user = userEvent.setup();

            render(<PrepaidSelectPlanDialog {...defaultProps} currentPlanName="Growth" hasActiveSubscription />);

            await user.click(screen.getByRole('button', {name: 'Select Starter'}));
            await user.click(screen.getByRole('button', {name: 'Keep current plan'}));

            expect(screen.queryByText('Downgrade to Starter?')).not.toBeInTheDocument();
            expect(hoisted.upgradeSubscription).not.toHaveBeenCalled();
        });
    });
});
