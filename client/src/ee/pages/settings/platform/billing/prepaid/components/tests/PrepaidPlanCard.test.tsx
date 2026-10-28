import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import PrepaidPlanCard from '../PrepaidPlanCard';

const defaultProps = {
    cancelAtPeriodEnd: false,
    jobsExecuted: 0,
    onChangePlan: vi.fn(),
    planName: 'Starter',
    planUnitTotal: 5000,
    planUnitUsed: 1250,
    productUnitLimit: 1000,
};

describe('PrepaidPlanCard', () => {
    it('shows plan units used and included', () => {
        render(<PrepaidPlanCard {...defaultProps} />);

        expect(screen.getByText('1,250')).toBeInTheDocument();
        expect(screen.getByText('Job executions used this period')).toBeInTheDocument();
        expect(screen.getByText('Used 1,250')).toBeInTheDocument();
        expect(screen.getByText('Included 5,000')).toBeInTheDocument();
    });

    it('renders no overage bar', () => {
        const {container} = render(<PrepaidPlanCard {...defaultProps} planUnitUsed={9000} />);

        expect(container.querySelector('.bg-orange-500')).toBeNull();
        expect(container.querySelector('.bg-blue-500')).toHaveStyle({width: '100%'});
        expect(screen.queryByText('Flat rate (included)')).not.toBeInTheDocument();
    });

    it('renders trial copy for trial', () => {
        render(
            <PrepaidPlanCard
                {...defaultProps}
                jobsExecuted={100}
                planName="Trial"
                productUnitLimit={1000}
                trialDaysRemaining={5}
            />
        );

        expect(screen.getByText('900')).toBeInTheDocument();
        expect(screen.getByText('Job executions available')).toBeInTheDocument();
        expect(screen.getByText('Spent 100')).toBeInTheDocument();
        expect(screen.getByText('Limit 1,000')).toBeInTheDocument();
    });

    it('renders orange overflow when trial executions exceed the limit', () => {
        const {container} = render(
            <PrepaidPlanCard
                {...defaultProps}
                jobsExecuted={1500}
                planName="Trial"
                productUnitLimit={1000}
                trialDaysRemaining={5}
            />
        );

        expect(container.querySelector('.bg-orange-500')).toHaveStyle({width: '100%'});
        expect(container.querySelector('.bg-blue-500')).toHaveStyle({width: '100%'});
    });

    it('renders an empty trial bar without dividing by zero when the limit is zero', () => {
        const {container} = render(
            <PrepaidPlanCard {...defaultProps} planName="Trial" productUnitLimit={0} trialDaysRemaining={5} />
        );

        expect(container.querySelector('.bg-orange-500')).toBeNull();
        expect(container.querySelector('.bg-blue-500')).toHaveStyle({width: '0%'});
    });
});
