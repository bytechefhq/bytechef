import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import SelectPlanDialog from '../SelectPlanDialog';

vi.mock('@/shared/mutations/platform/billing.mutations', () => ({
    useUpgradeSubscriptionMutation: () => ({mutate: vi.fn(), reset: vi.fn()}),
}));

vi.mock('@/shared/middleware/platform/billing', () => ({
    BillingApi: class {
        createCheckoutSession = vi.fn();
    },
    CheckoutSessionRequestPlanNameEnum: {Enterprise: 'ENTERPRISE', Starter: 'STARTER', Team: 'TEAM'},
}));

vi.mock('../PlanTierCard', () => ({
    default: ({name}: {name: string}) => <div data-testid="plan-tier-card">{name}</div>,
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

describe('SelectPlanDialog', () => {
    describe('rendering', () => {
        it('should render the title and description', () => {
            render(<SelectPlanDialog {...defaultProps} />);

            expect(screen.getByRole('heading', {name: 'Select a plan'})).toBeInTheDocument();
            expect(screen.getByText('You can upgrade, downgrade, or cancel at any time.')).toBeInTheDocument();
        });

        it('should render the plan cards', () => {
            render(<SelectPlanDialog {...defaultProps} />);

            expect(screen.getAllByTestId('plan-tier-card').length).toBeGreaterThan(0);
        });

        it('should not render when closed', () => {
            render(<SelectPlanDialog {...defaultProps} open={false} />);

            expect(screen.queryByRole('heading', {name: 'Select a plan'})).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        // The compare-plans action lives in the header's endContent slot, beside
        // the close button the family provides.
        it('should render the compare plans action and the close control', () => {
            render(<SelectPlanDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Compare plans'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should open the pricing page from compare plans', async () => {
            const user = userEvent.setup();

            render(<SelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Compare plans'}));

            expect(window.open).toHaveBeenCalledWith('https://bytechef.io/pricing', '_blank');
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<SelectPlanDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
