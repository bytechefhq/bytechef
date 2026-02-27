import {CheckIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface DataStreamStepNavProps {
    configuredSteps: Set<number>;
    currentStep: number;
    onGoToStep: (step: number) => void;
    stepLabels: string[];
}

export default function DataStreamStepNav({
    configuredSteps,
    currentStep,
    onGoToStep,
    stepLabels,
}: DataStreamStepNavProps) {
    return (
        <div className="flex items-center justify-center gap-2 px-4 py-3">
            {stepLabels.map((label, index) => {
                const isConfigured = configuredSteps.has(index);
                const isCurrent = index === currentStep;
                const isClickable = index === 0 || isConfigured || configuredSteps.has(index - 1);

                return (
                    <div className="flex items-center gap-2" key={label}>
                        {index > 0 && <div className="h-px w-8 bg-border" />}

                        <button
                            className={twMerge(
                                'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm transition-colors',
                                isCurrent && 'bg-primary/10 font-medium text-primary',
                                !isCurrent &&
                                    isClickable &&
                                    'cursor-pointer text-muted-foreground hover:text-foreground',
                                !isCurrent && !isClickable && 'cursor-default text-muted-foreground/50'
                            )}
                            disabled={!isClickable}
                            onClick={() => isClickable && onGoToStep(index)}
                            type="button"
                        >
                            <div
                                className={twMerge(
                                    'flex size-6 items-center justify-center rounded-full text-xs',
                                    isCurrent && 'bg-surface-brand-primary text-content-onsurface-primary',
                                    !isCurrent &&
                                        isConfigured &&
                                        'bg-surface-brand-primary/20 text-surface-brand-primary',
                                    !isCurrent && !isConfigured && 'bg-muted text-muted-foreground'
                                )}
                            >
                                {isConfigured && !isCurrent ? <CheckIcon className="size-3.5" /> : index + 1}
                            </div>

                            <span>{label}</span>
                        </button>
                    </div>
                );
            })}
        </div>
    );
}
