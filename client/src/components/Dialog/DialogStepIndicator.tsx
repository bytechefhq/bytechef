import {Progress as ShadcnProgress} from '@/components/ui/progress';
import {type ComponentPropsWithRef} from 'react';
import {twMerge} from 'tailwind-merge';

import {useDialogSteps} from './hooks/useDialogSteps';

interface DialogStepIndicatorProps extends ComponentPropsWithRef<'div'> {
    showLabel?: boolean;
}

function DialogStepIndicator({className, showLabel = true, ...props}: DialogStepIndicatorProps) {
    const {currentStepIndex, steps} = useDialogSteps();

    const stepNumber = currentStepIndex + 1;
    const label = `Step ${stepNumber} of ${steps.length}`;

    return (
        <div
            className={twMerge('mt-auto flex min-w-32 flex-col gap-2', className)}
            data-slot="dialog-step-indicator"
            {...props}
        >
            {showLabel && <span className="text-xs leading-4 font-medium text-content-neutral-primary">{label}</span>}

            <ShadcnProgress
                aria-label={label}
                aria-valuenow={(stepNumber / steps.length) * 100}
                className="h-2 bg-surface-brand-secondary *:data-[slot=progress-indicator]:bg-surface-brand-primary"
                value={(stepNumber / steps.length) * 100}
            />
        </div>
    );
}

DialogStepIndicator.displayName = 'DialogStepIndicator';

export {DialogStepIndicator};
export type {DialogStepIndicatorProps};
