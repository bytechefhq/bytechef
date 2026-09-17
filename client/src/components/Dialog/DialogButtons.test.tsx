import {DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {Dialog, DialogContent} from './Dialog';
import {DialogCancelButton, DialogNextButton, DialogPreviousButton} from './DialogButtons';
import {type DialogStepI, DialogStepsProvider, type DialogStepsProviderProps} from './DialogStepsProvider';
import {useDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'review', label: 'Review'},
];

type ProviderOptionsType = Partial<Omit<DialogStepsProviderProps, 'children'>>;

function CurrentStepProbe() {
    const {currentStep} = useDialogSteps();

    return <span data-testid="current-step">{currentStep.id}</span>;
}

function renderNextButton(providerOptions: ProviderOptionsType = {}, lastStepLabel?: string) {
    return render(
        <DialogStepsProvider steps={steps} {...providerOptions}>
            <DialogNextButton lastStepLabel={lastStepLabel} />

            <CurrentStepProbe />
        </DialogStepsProvider>
    );
}

describe('DialogNextButton', () => {
    it('should advance to the next step', async () => {
        renderNextButton();

        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        expect(screen.getByTestId('current-step')).toHaveTextContent('review');
    });

    it('should be a non-submitting button', () => {
        renderNextButton();

        expect(screen.getByRole('button', {name: 'Continue'})).toHaveAttribute('type', 'button');
    });

    it('should be disabled when the current step cannot proceed', () => {
        renderNextButton({steps: [{canProceed: false, id: 'basics', label: 'Basics'}, steps[1]]});

        expect(screen.getByRole('button', {name: 'Continue'})).toBeDisabled();
    });

    it('should show a spinner and disable itself while pending', async () => {
        let resolveValidation: (isValid: boolean) => void = () => undefined;

        const validateStep = vi.fn(
            () =>
                new Promise<boolean>((resolve) => {
                    resolveValidation = resolve;
                })
        );

        renderNextButton({validateStep});

        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        const continueButton = screen.getByRole('button', {name: 'Continue'});

        expect(continueButton).toBeDisabled();
        expect(continueButton.querySelector('svg.animate-spin')).toBeInTheDocument();

        await act(async () => {
            resolveValidation(true);
        });

        expect(screen.getByTestId('current-step')).toHaveTextContent('review');
    });

    it('should use lastStepLabel on the last step and call onComplete', async () => {
        const onComplete = vi.fn();

        renderNextButton({defaultStepId: 'review', onComplete}, 'Save');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(onComplete).toHaveBeenCalledTimes(1);
    });

    it('should keep label on the last step when lastStepLabel is missing', () => {
        renderNextButton({defaultStepId: 'review'});

        expect(screen.getByRole('button', {name: 'Continue'})).toBeInTheDocument();
    });
});

describe('DialogNextButton - External pending', () => {
    it('should keep the spinner while work started by onComplete is still running', () => {
        render(
            <DialogStepsProvider defaultStepId="review" steps={steps}>
                <DialogNextButton isPending lastStepLabel="Save" />
            </DialogStepsProvider>
        );

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeDisabled();
        expect(saveButton.querySelector('svg.animate-spin')).toBeInTheDocument();
    });

    it('should report a rejected step transition instead of throwing at the click handler', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const validateStep = vi.fn().mockRejectedValue(new Error('Validation failed'));

        renderNextButton({validateStep});

        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        expect(consoleErrorSpy).toHaveBeenCalledWith(
            'DialogNextButton failed to advance the dialog:',
            expect.any(Error)
        );
        expect(screen.getByTestId('current-step')).toHaveTextContent('basics');

        consoleErrorSpy.mockRestore();
    });
});

describe('DialogPreviousButton', () => {
    function renderPreviousButton(providerOptions: ProviderOptionsType = {}) {
        return render(
            <DialogStepsProvider steps={steps} {...providerOptions}>
                <DialogNextButton />

                <DialogPreviousButton />

                <CurrentStepProbe />
            </DialogStepsProvider>
        );
    }

    it('should not render on the first step', () => {
        renderPreviousButton();

        expect(screen.queryByRole('button', {name: 'Previous'})).not.toBeInTheDocument();
    });

    it('should go back to the previous step', async () => {
        renderPreviousButton();

        await userEvent.click(screen.getByRole('button', {name: 'Continue'}));

        expect(screen.getByTestId('current-step')).toHaveTextContent('review');

        await userEvent.click(screen.getByRole('button', {name: 'Previous'}));

        expect(screen.getByTestId('current-step')).toHaveTextContent('basics');
    });
});

describe('DialogCancelButton', () => {
    function renderCancelButton(onOpenChange = vi.fn(), label?: string) {
        return render(
            <Dialog onOpenChange={onOpenChange} open>
                <DialogContent aria-describedby={undefined}>
                    <ShadcnDialogTitle>Title</ShadcnDialogTitle>

                    <DialogCancelButton label={label} />
                </DialogContent>
            </Dialog>
        );
    }

    it('should close the dialog', async () => {
        const handleOpenChange = vi.fn();

        renderCancelButton(handleOpenChange);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(handleOpenChange).toHaveBeenCalledWith(false);
    });

    it('should render a custom label with the outline variant', () => {
        renderCancelButton(vi.fn(), 'Discard');

        expect(screen.getByRole('button', {name: 'Discard'})).toHaveClass('border-stroke-neutral-secondary');
    });
});

describe('Dialog buttons - TypeScript', () => {
    it('should not accept onClick on DialogNextButton', () => {
        // @ts-expect-error - DialogNextButton drives its own click
        const element = <DialogNextButton onClick={() => undefined} />;

        expect(element).toBeTruthy();
    });

    it('should not accept variant on DialogCancelButton', () => {
        // @ts-expect-error - DialogCancelButton is always the outline variant
        const element = <DialogCancelButton variant="default" />;

        expect(element).toBeTruthy();
    });
});
