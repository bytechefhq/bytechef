import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PrepaidSelectPlanDialog from '../PrepaidSelectPlanDialog';

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useUpgradeSubscriptionMutation: () => ({mutate: vi.fn(), reset: vi.fn()}),
}));

vi.mock('@/shared/middleware/platform/billing', () => ({
    BillingApi: class {
        createCheckoutSession = vi.fn();
    },
    CheckoutSessionRequestPlanNameEnum: {Enterprise: 'ENTERPRISE', Growth: 'GROWTH', Starter: 'STARTER', Team: 'TEAM'},
}));

vi.mock('../PrepaidPlanTierCard', () => ({
    default: ({features, name}: {features: string[]; name: string}) => (
        <div data-testid="plan-tier-card">
            {name}

            <ul>
                {features.map((feature) => (
                    <li key={feature}>{feature}</li>
                ))}
            </ul>
        </div>
    ),
}));

const onClose = vi.fn();

const defaultProps = {hasActiveSubscription: false, onClose, open: true};

beforeEach(() => {
    windowResizeObserver();
    vi.spyOn(window, 'open').mockImplementation(() => null);
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('PrepaidSelectPlanDialog', () => {
    describe('rendering', () => {
        it('should render the title and description', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getByRole('heading', {name: 'Select a plan'})).toBeInTheDocument();
            expect(screen.getByText('You can upgrade, downgrade, or cancel at any time.')).toBeInTheDocument();
        });

        it('should render the plan cards', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} />);

            expect(screen.getAllByTestId('plan-tier-card').length).toBeGreaterThan(0);
        });

        it('should not render when closed', () => {
            render(<PrepaidSelectPlanDialog {...defaultProps} open={false} />);

            expect(screen.queryByRole('heading', {name: 'Select a plan'})).not.toBeInTheDocument();
        });
    });

    describe('prepaid copy', () => {
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
    });
});
