import {render, screen, userEvent} from '@/shared/util/test-utils';
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

    it('renders an empty trial bar when executions ran without a limit', () => {
        const {container} = render(
            <PrepaidPlanCard
                {...defaultProps}
                jobsExecuted={10}
                planName="Trial"
                productUnitLimit={0}
                trialDaysRemaining={5}
            />
        );

        expect(container.querySelector('.bg-blue-500')).toHaveStyle({width: '0%'});
        expect(container.querySelector('.bg-orange-500')).toHaveStyle({width: '100%'});
    });

    it('renders an empty plan bar when the plan includes no units', () => {
        const {container} = render(<PrepaidPlanCard {...defaultProps} planUnitTotal={0} planUnitUsed={0} />);

        expect(container.querySelector('.bg-blue-500')).toHaveStyle({width: '0%'});
    });

    it('shows when an active plan renews', () => {
        render(<PrepaidPlanCard {...defaultProps} planName="STARTER" renewalDate="December 1, 2026" />);

        expect(screen.getByText(/plan renews on/)).toBeInTheDocument();
        expect(screen.getByText('Starter')).toBeInTheDocument();
        expect(screen.getByText('December 1, 2026')).toBeInTheDocument();
    });

    it('shows a scheduled downgrade', () => {
        render(
            <PrepaidPlanCard
                {...defaultProps}
                planName="GROWTH"
                renewalDate="December 1, 2026"
                scheduledPlanName="STARTER"
            />
        );

        expect(screen.getByText(/Your plan will downgrade to/)).toBeInTheDocument();
        expect(screen.getByText('Starter')).toBeInTheDocument();
    });

    it('shows when a plan cancelled at period end ends and offers reactivation', async () => {
        const onReactivatePlan = vi.fn();

        render(
            <PrepaidPlanCard
                {...defaultProps}
                cancelAtPeriodEnd
                onReactivatePlan={onReactivatePlan}
                renewalDate="December 1, 2026"
            />
        );

        expect(screen.getByText(/Your subscription will be cancelled on/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Change plan'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

        expect(onReactivatePlan).toHaveBeenCalledTimes(1);
    });

    it('changes and cancels an active plan', async () => {
        const onCancelPlan = vi.fn();
        const onChangePlan = vi.fn();

        render(<PrepaidPlanCard {...defaultProps} onCancelPlan={onCancelPlan} onChangePlan={onChangePlan} />);

        await userEvent.click(screen.getByRole('button', {name: 'Change plan'}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

        expect(onChangePlan).toHaveBeenCalledTimes(1);
        expect(onCancelPlan).toHaveBeenCalledTimes(1);
    });

    it('shows an expired trial without usage', () => {
        render(<PrepaidPlanCard {...defaultProps} planName="Trial" trialDaysRemaining={0} trialExpired />);

        expect(screen.getByText('Your trial has expired.')).toBeInTheDocument();
        expect(screen.queryByText(/Job executions/)).not.toBeInTheDocument();
    });

    it('shows the remaining trial days and executions', () => {
        render(<PrepaidPlanCard {...defaultProps} planName="Trial" trialDaysRemaining={5} />);

        expect(screen.getByText('5 days')).toBeInTheDocument();
        expect(screen.getByText('1,000 job executions')).toBeInTheDocument();
    });
});
