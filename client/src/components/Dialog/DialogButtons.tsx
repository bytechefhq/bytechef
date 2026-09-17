import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {DialogClose as ShadcnDialogClose} from '@/components/ui/dialog';

import {useDialogSteps} from './hooks/useDialogSteps';

interface DialogNextButtonProps {
    className?: string;
    isPending?: boolean;
    label?: string;

    lastStepLabel?: string;
}

interface DialogPreviousButtonProps {
    className?: string;
    label?: string;
}

interface DialogCancelButtonProps {
    className?: string;
    label?: string;
}

function DialogNextButton({
    className,
    isPending: isExternalPending = false,
    label = 'Continue',
    lastStepLabel,
}: DialogNextButtonProps) {
    const {currentStep, goToNextStep, isLastStep, isPending} = useDialogSteps();

    const buttonLabel = isLastStep ? lastStepLabel || label : label;
    const isBusy = isPending || isExternalPending;

    const handleClick = () => {
        goToNextStep().catch((error) => console.error('DialogNextButton failed to advance the dialog:', error));
    };

    return (
        <Button
            className={className}
            disabled={isBusy || currentStep.canProceed === false}
            icon={isBusy ? <LoadingIcon /> : undefined}
            label={buttonLabel}
            onClick={handleClick}
            type="button"
        />
    );
}

DialogNextButton.displayName = 'DialogNextButton';

function DialogPreviousButton({className, label = 'Previous'}: DialogPreviousButtonProps) {
    const {goToPreviousStep, isFirstStep, isPending} = useDialogSteps();

    if (isFirstStep) {
        return null;
    }

    return (
        <Button
            className={className}
            disabled={isPending}
            label={label}
            onClick={goToPreviousStep}
            type="button"
            variant="outline"
        />
    );
}

DialogPreviousButton.displayName = 'DialogPreviousButton';

function DialogCancelButton({className, label = 'Cancel'}: DialogCancelButtonProps) {
    return (
        <ShadcnDialogClose asChild>
            <Button className={className} label={label} type="button" variant="outline" />
        </ShadcnDialogClose>
    );
}

DialogCancelButton.displayName = 'DialogCancelButton';

export {DialogCancelButton, DialogNextButton, DialogPreviousButton};
export type {DialogCancelButtonProps, DialogNextButtonProps, DialogPreviousButtonProps};
