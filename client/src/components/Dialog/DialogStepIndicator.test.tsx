import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {DialogStepIndicator} from './DialogStepIndicator';
import {type DialogStepI, DialogStepsProvider} from './DialogStepsProvider';
import {useDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'review', label: 'Review'},
];

function NextStepProbe() {
    const {goToNextStep} = useDialogSteps();

    return (
        <button onClick={() => goToNextStep()} type="button">
            Next step probe
        </button>
    );
}

function renderIndicator(showLabel?: boolean, className?: string) {
    return render(
        <DialogStepsProvider steps={steps}>
            <DialogStepIndicator className={className} showLabel={showLabel} />

            <NextStepProbe />
        </DialogStepsProvider>
    );
}

describe('DialogStepIndicator', () => {
    it('should show the step position and progress', () => {
        renderIndicator();

        expect(screen.getByText('Step 1 of 2')).toHaveClass(
            'text-xs',
            'leading-4',
            'font-medium',
            'text-content-neutral-primary'
        );
        expect(screen.getByRole('progressbar', {name: 'Step 1 of 2'})).toHaveAttribute('aria-valuenow', '50');
    });

    it('should restyle the progress bar with tokens', () => {
        renderIndicator();

        expect(screen.getByRole('progressbar')).toHaveClass(
            'h-2',
            'bg-surface-brand-secondary',
            '*:data-[slot=progress-indicator]:bg-surface-brand-primary'
        );
    });

    it('should follow the current step', async () => {
        renderIndicator();

        await userEvent.click(screen.getByRole('button', {name: 'Next step probe'}));

        expect(screen.getByText('Step 2 of 2')).toBeInTheDocument();
        expect(screen.getByRole('progressbar', {name: 'Step 2 of 2'})).toHaveAttribute('aria-valuenow', '100');
    });

    it('should hide the label but keep the progress name when showLabel is false', () => {
        renderIndicator(false);

        expect(screen.queryByText('Step 1 of 2')).not.toBeInTheDocument();
        expect(screen.getByRole('progressbar', {name: 'Step 1 of 2'})).toBeInTheDocument();
    });

    it('should pin itself to the bottom of the sidebar and merge className', () => {
        renderIndicator(true, 'px-1');

        const indicator = screen.getByRole('progressbar').parentElement;

        expect(indicator).toHaveAttribute('data-slot', 'dialog-step-indicator');
        expect(indicator).toHaveClass('mt-auto', 'flex', 'min-w-32', 'flex-col', 'gap-2', 'px-1');
    });
});
