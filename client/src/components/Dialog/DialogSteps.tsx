import Button from '@/components/Button/Button';
import {CircleCheckIcon, CircleIcon} from 'lucide-react';
import {type ComponentPropsWithRef} from 'react';
import {twMerge} from 'tailwind-merge';

import {useDialogSteps} from './hooks/useDialogSteps';

type DialogStepsPropsType = ComponentPropsWithRef<'nav'>;

const stepStyles =
    'w-full justify-start px-3 text-content-neutral-secondary hover:text-content-neutral-secondary focus-visible:ring-2 focus-visible:ring-stroke-brand-focus aria-disabled:cursor-not-allowed';

const currentStepStyles =
    'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary hover:text-content-brand-primary';

const DialogSteps = ({'aria-label': ariaLabel = 'Steps', className, ...props}: DialogStepsPropsType) => {
    const {getStepStatus, goToStep, steps} = useDialogSteps();

    return (
        <nav aria-label={ariaLabel} className={twMerge('flex flex-col', className)} data-slot="dialog-steps" {...props}>
            <ol className="flex flex-col gap-2">
                {steps.map((step) => {
                    const {isCompleted, isCurrent, isReachable} = getStepStatus(step.id);

                    return (
                        <li key={step.id}>
                            <Button
                                aria-current={isCurrent ? 'step' : undefined}
                                aria-disabled={!isReachable && !isCurrent ? true : undefined}
                                className={twMerge(stepStyles, isCurrent && currentStepStyles)}
                                icon={
                                    isCompleted ? (
                                        <CircleCheckIcon aria-hidden="true" />
                                    ) : (
                                        <CircleIcon aria-hidden="true" />
                                    )
                                }
                                onClick={() => goToStep(step.id)}
                                type="button"
                                variant="ghost"
                            >
                                <span>{step.label}</span>

                                {isCompleted && <span className="sr-only">(completed)</span>}
                            </Button>
                        </li>
                    );
                })}
            </ol>
        </nav>
    );
};

DialogSteps.displayName = 'DialogSteps';

export {DialogSteps};
export type {DialogStepsPropsType as DialogStepsProps};
