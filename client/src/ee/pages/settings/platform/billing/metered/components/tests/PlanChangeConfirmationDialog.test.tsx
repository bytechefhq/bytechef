import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PlanChangeConfirmationDialog from '../PlanChangeConfirmationDialog';

const onClose = vi.fn();
const onConfirm = vi.fn();

const defaultProps = {
    currentPlanName: 'Growth',
    isPending: false,
    newPlanName: 'Starter',
    onClose,
    onConfirm,
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('PlanChangeConfirmationDialog', () => {
    it('should describe a downgrade taking effect at the end of the billing period', () => {
        render(<PlanChangeConfirmationDialog {...defaultProps} direction="downgrade" />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Downgrade to Starter?');
        expect(screen.getByText(/You are downgrading from/)).toHaveTextContent(
            'You are downgrading from Growth to Starter.'
        );
        expect(
            screen.getByText(
                'This change will take effect at the end of your current billing period. You will retain access to your current plan features until then.'
            )
        ).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Confirm downgrade'})).toHaveClass('bg-surface-destructive-primary');
    });

    it('should describe an upgrade being charged immediately', () => {
        render(
            <PlanChangeConfirmationDialog
                {...defaultProps}
                currentPlanName="Starter"
                direction="upgrade"
                newPlanName="Growth"
            />
        );

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Upgrade to Growth?');
        expect(screen.getByText(/You are upgrading from/)).toHaveTextContent(
            'You are upgrading from Starter to Growth.'
        );
        expect(
            screen.getByText(
                'You will be charged immediately for the prorated cost for the remainder of your current billing period. This action cannot be undone.'
            )
        ).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upgrade now'})).toHaveClass('bg-surface-brand-primary');
    });

    it('should omit the plan comparison when the current plan is unknown', () => {
        render(<PlanChangeConfirmationDialog {...defaultProps} currentPlanName={undefined} direction="downgrade" />);

        expect(screen.queryByText(/You are downgrading/)).not.toBeInTheDocument();
    });

    it('should show the pending label for the direction while the change is in flight', () => {
        const {rerender} = render(<PlanChangeConfirmationDialog {...defaultProps} direction="downgrade" isPending />);

        expect(screen.getByRole('button', {name: 'Downgrading…'})).toBeDisabled();

        rerender(<PlanChangeConfirmationDialog {...defaultProps} direction="upgrade" isPending />);

        expect(screen.getByRole('button', {name: 'Upgrading…'})).toBeDisabled();
    });

    it('should call onConfirm and onClose from the footer actions', async () => {
        render(<PlanChangeConfirmationDialog {...defaultProps} direction="downgrade" />);

        await userEvent.click(screen.getByRole('button', {name: 'Confirm downgrade'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);

        await userEvent.click(screen.getByRole('button', {name: 'Keep current plan'}));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
