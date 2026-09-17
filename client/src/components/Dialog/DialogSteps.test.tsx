import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {DialogSteps} from './DialogSteps';
import {type DialogStepI, DialogStepsProvider} from './DialogStepsProvider';
import {useDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'workflows', label: 'Workflows'},
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

function renderSteps(ariaLabel?: string, className?: string) {
    return render(
        <DialogStepsProvider steps={steps}>
            <DialogSteps aria-label={ariaLabel} className={className} />

            <NextStepProbe />
        </DialogStepsProvider>
    );
}

describe('DialogSteps - Rendering', () => {
    it('should render a navigation list of every step', () => {
        renderSteps();

        const navigation = screen.getByRole('navigation', {name: 'Steps'});

        expect(navigation).toHaveAttribute('data-slot', 'dialog-steps');
        expect(within(navigation).getAllByRole('button')).toHaveLength(3);
    });

    it('should mark the current step', () => {
        renderSteps();

        const basicsButton = screen.getByRole('button', {name: /Basics/});

        expect(basicsButton).toHaveAttribute('aria-current', 'step');
        expect(basicsButton).toHaveClass('bg-surface-brand-secondary', 'text-content-brand-primary');
        expect(screen.getByRole('button', {name: /Workflows/})).not.toHaveAttribute('aria-current');
    });

    it('should use a custom aria-label and merge className', () => {
        renderSteps('Deployment steps', 'px-2');

        expect(screen.getByRole('navigation', {name: 'Deployment steps'})).toHaveClass('px-2');
    });
});

describe('DialogSteps - Gating', () => {
    it('should mark locked steps as aria-disabled while keeping them focusable', () => {
        renderSteps();

        expect(screen.getByRole('button', {name: /Basics/})).not.toHaveAttribute('aria-disabled');
        expect(screen.getByRole('button', {name: /Workflows/})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('button', {name: /Review/})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('button', {name: /Review/})).toBeEnabled();
    });

    it('should ignore clicks on a locked step', async () => {
        renderSteps();

        await userEvent.click(screen.getByRole('button', {name: /Review/}));

        expect(screen.getByRole('button', {name: /Basics/})).toHaveAttribute('aria-current', 'step');
    });

    it('should move to a completed step when it is clicked', async () => {
        renderSteps();

        await userEvent.click(screen.getByRole('button', {name: 'Next step probe'}));

        expect(screen.getByRole('button', {name: /Workflows/})).toHaveAttribute('aria-current', 'step');

        await userEvent.click(screen.getByRole('button', {name: /Basics/}));

        expect(screen.getByRole('button', {name: /Basics/})).toHaveAttribute('aria-current', 'step');
        expect(screen.getByRole('button', {name: /Workflows/})).not.toHaveAttribute('aria-disabled');
    });

    it('should announce completed steps to screen readers', async () => {
        renderSteps();

        await userEvent.click(screen.getByRole('button', {name: 'Next step probe'}));

        const basicsButton = screen.getByRole('button', {name: /Basics/});

        expect(within(basicsButton).getByText('(completed)')).toHaveClass('sr-only');
        expect(screen.getByRole('button', {name: /Workflows/})).not.toHaveTextContent('(completed)');
    });
});
