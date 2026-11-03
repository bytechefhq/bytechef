import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import PrepaidPlanTierCard from '../PrepaidPlanTierCard';

const defaultProps = {
    ctaLabel: 'Get Started',
    description: 'Perfect for individuals and small businesses.',
    features: ['Unlimited workflows', '1 workspace'],
    name: 'Starter',
    onSelect: vi.fn(),
    price: '$29',
};

describe('PrepaidPlanTierCard', () => {
    it('shows the plan name, description, monthly price and features', () => {
        render(<PrepaidPlanTierCard {...defaultProps} />);

        expect(screen.getByText('Starter')).toBeInTheDocument();
        expect(screen.getByText('Perfect for individuals and small businesses.')).toBeInTheDocument();
        expect(screen.getByText('$29')).toBeInTheDocument();
        expect(screen.getByText('/ Month')).toBeInTheDocument();
        expect(screen.getByText('Unlimited workflows')).toBeInTheDocument();
        expect(screen.getByText('1 workspace')).toBeInTheDocument();
    });

    it('selects the plan from the call to action', async () => {
        const onSelect = vi.fn();

        render(<PrepaidPlanTierCard {...defaultProps} onSelect={onSelect} />);

        await userEvent.click(screen.getByRole('button', {name: 'Get Started'}));

        expect(onSelect).toHaveBeenCalledTimes(1);
    });

    it('shows the call to action in place of a price for a plan without one', () => {
        render(<PrepaidPlanTierCard {...defaultProps} ctaLabel="Contact Us" highlighted price={null} />);

        expect(screen.getAllByText('Contact Us')).toHaveLength(2);
        expect(screen.queryByText('/ Month')).not.toBeInTheDocument();
    });

    it('disables the current plan', () => {
        render(<PrepaidPlanTierCard {...defaultProps} isCurrent />);

        expect(screen.getByRole('button', {name: 'Current Plan'})).toBeDisabled();
    });

    it('marks a disabled plan as coming soon', () => {
        render(<PrepaidPlanTierCard {...defaultProps} disabled />);

        expect(screen.getByRole('button', {name: 'Coming Soon!'})).toBeDisabled();
    });
});
