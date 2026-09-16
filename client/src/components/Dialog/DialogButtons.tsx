import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {DialogClose as ShadcnDialogClose} from '@/components/ui/dialog';

import {useDialogSteps} from './hooks/useDialogSteps';

interface DialogNextButtonProps {
    className?: string;
    label?: string;

    lastStepLabel?: string;
}

interface DialogCancelButtonProps {
    className?: string;
    label?: string;
}

function DialogNextButton({className, label = 'Continue', lastStepLabel}: DialogNextButtonProps) {
    const {currentStep, goToNextStep, isLastStep, isPending} = useDialogSteps();

    const buttonLabel = isLastStep ? lastStepLabel || label : label;

    return (
        <Button
            className={className}
            disabled={isPending || currentStep.canProceed === false}
            icon={isPending ? <LoadingIcon /> : undefined}
            label={buttonLabel}
            onClick={() => goToNextStep()}
            type="button"
        />
    );
}

DialogNextButton.displayName = 'DialogNextButton';

function DialogCancelButton({className, label = 'Cancel'}: DialogCancelButtonProps) {
    return (
        <ShadcnDialogClose asChild>
            <Button className={className} label={label} type="button" variant="outline" />
        </ShadcnDialogClose>
    );
}

DialogCancelButton.displayName = 'DialogCancelButton';

export {DialogCancelButton, DialogNextButton};
export type {DialogCancelButtonProps, DialogNextButtonProps};
