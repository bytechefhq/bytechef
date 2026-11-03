import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import PlanCard from '../PlanCard';

const defaultProps = {
    cancelAtPeriodEnd: false,
    jobsExecuted: 400,
    onChangePlan: vi.fn(),
    planName: 'STARTER',
    productUnitLimit: 1000,
    renewalDate: 'December 1, 2026',
};

describe('PlanCard', () => {
    describe('status', () => {
        it('shows when an active plan renews', () => {
            render(<PlanCard {...defaultProps} />);

            expect(screen.getByText('Starter')).toBeInTheDocument();
            expect(screen.getByText('December 1, 2026')).toBeInTheDocument();
            expect(screen.getByText(/plan renews on/)).toBeInTheDocument();
        });

        it('shows a scheduled downgrade', () => {
            render(<PlanCard {...defaultProps} planName="GROWTH" scheduledPlanName="STARTER" />);

            expect(screen.getByText(/Your plan will downgrade to/)).toBeInTheDocument();
            expect(screen.getByText('Starter')).toBeInTheDocument();
        });

        it('shows when a plan cancelled at period end ends', () => {
            render(<PlanCard {...defaultProps} cancelAtPeriodEnd />);

            expect(screen.getByText(/Your subscription will be cancelled on/)).toBeInTheDocument();
            expect(screen.getByText('December 1, 2026')).toBeInTheDocument();
        });

        it('shows the remaining trial days and executions', () => {
            render(<PlanCard {...defaultProps} planName="Trial" renewalDate={undefined} trialDaysRemaining={5} />);

            expect(screen.getByText('5 days')).toBeInTheDocument();
            expect(screen.getByText('1,000 job executions')).toBeInTheDocument();
        });

        it('shows an expired trial without usage', () => {
            render(
                <PlanCard
                    {...defaultProps}
                    planName="Trial"
                    renewalDate={undefined}
                    trialDaysRemaining={0}
                    trialExpired
                />
            );

            expect(screen.getByText('Your trial has expired.')).toBeInTheDocument();
            expect(screen.queryByText(/Job executions/)).not.toBeInTheDocument();
        });

        it('omits the status line when nothing is known about the period', () => {
            render(<PlanCard {...defaultProps} renewalDate={undefined} />);

            expect(screen.queryByText(/renews on/)).not.toBeInTheDocument();
        });
    });

    describe('actions', () => {
        it('changes and cancels an active plan', async () => {
            const onCancelPlan = vi.fn();
            const onChangePlan = vi.fn();

            render(<PlanCard {...defaultProps} onCancelPlan={onCancelPlan} onChangePlan={onChangePlan} />);

            await userEvent.click(screen.getByRole('button', {name: 'Change plan'}));
            await userEvent.click(screen.getByRole('button', {name: 'Cancel plan'}));

            expect(onChangePlan).toHaveBeenCalledTimes(1);
            expect(onCancelPlan).toHaveBeenCalledTimes(1);
        });

        it('reactivates a plan cancelled at period end', async () => {
            const onReactivatePlan = vi.fn();

            render(<PlanCard {...defaultProps} cancelAtPeriodEnd onReactivatePlan={onReactivatePlan} />);

            expect(screen.queryByRole('button', {name: 'Change plan'})).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Reactivate'}));

            expect(onReactivatePlan).toHaveBeenCalledTimes(1);
        });

        it('shows no action for a cancelled plan that cannot be reactivated', () => {
            render(<PlanCard {...defaultProps} cancelAtPeriodEnd />);

            expect(screen.queryByRole('button')).not.toBeInTheDocument();
        });
    });

    describe('usage', () => {
        it('shows usage within the included executions', () => {
            const {container} = render(<PlanCard {...defaultProps} />);

            expect(screen.getByText('400')).toBeInTheDocument();
            expect(screen.getByText('Job executions used this period')).toBeInTheDocument();
            expect(screen.getByText('400 flat rate')).toBeInTheDocument();
            expect(screen.getByText('1,000 included')).toBeInTheDocument();
            expect(screen.getByText('Flat rate (included)')).toBeInTheDocument();
            expect(screen.queryByText(/Usage billed/)).not.toBeInTheDocument();
            expect(container.querySelector('.bg-orange-500')).toBeNull();
        });

        it('shows usage billed beyond the included executions', () => {
            const {container} = render(<PlanCard {...defaultProps} jobsExecuted={1500} />);

            expect(screen.getByText('1,000 flat rate')).toBeInTheDocument();
            expect(screen.getByText('500 usage billed')).toBeInTheDocument();
            expect(screen.getByText('Usage billed ($1 / 1,000 job executions)')).toBeInTheDocument();
            expect(container.querySelector('.h-full.bg-orange-500')).toHaveStyle({width: '100%'});
        });

        it('shows available trial executions', () => {
            render(
                <PlanCard
                    {...defaultProps}
                    jobsExecuted={100}
                    planName="Trial"
                    renewalDate={undefined}
                    trialDaysRemaining={5}
                />
            );

            expect(screen.getByText('900')).toBeInTheDocument();
            expect(screen.getByText('Job executions available')).toBeInTheDocument();
            expect(screen.getByText('Spent 100')).toBeInTheDocument();
            expect(screen.getByText('Limit 1,000')).toBeInTheDocument();
            expect(screen.queryByText('Flat rate (included)')).not.toBeInTheDocument();
        });
    });
});
