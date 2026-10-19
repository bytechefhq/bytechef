import {CheckIcon} from 'lucide-react';
import {ComponentPropsWithRef, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

type JourneyStepStatusType = 'complete' | 'current' | 'upcoming';

type OrientationType = 'horizontal' | 'vertical';

interface JourneyStepI {
    action?: ReactNode;
    description?: string;
    id: string;
    label: string;
    status: JourneyStepStatusType;
}

interface JourneyStepsProps extends Omit<ComponentPropsWithRef<'ol'>, 'children'> {
    orientation?: OrientationType;
    steps: JourneyStepI[];
}

const markerStyles: Record<JourneyStepStatusType, string> = {
    complete: 'bg-surface-success-secondary text-content-success-primary',
    current: 'bg-surface-brand-primary text-content-onsurface-primary',
    upcoming: 'border border-stroke-neutral-secondary text-content-neutral-secondary',
};

const labelStyles: Record<JourneyStepStatusType, string> = {
    complete: 'text-content-neutral-secondary',
    current: 'font-semibold text-content-neutral-primary',
    upcoming: 'text-content-neutral-secondary',
};

const listStyles: Record<OrientationType, string> = {
    horizontal: 'flex flex-wrap items-center justify-center gap-y-2',
    vertical:
        'w-full divide-y divide-stroke-neutral-secondary rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-primary text-left',
};

const itemStyles: Record<OrientationType, string> = {
    horizontal: 'flex items-center gap-1.5 text-xs',
    vertical: 'grid grid-cols-[20px_1fr_auto] items-center gap-3 px-3 py-2.5 text-sm',
};

const JourneyStepMarker = ({position, status}: {position: number; status: JourneyStepStatusType}) => (
    <span
        aria-hidden="true"
        className={twMerge(
            'flex size-5 shrink-0 items-center justify-center rounded-full text-xs font-semibold',
            markerStyles[status]
        )}
    >
        {status === 'complete' ? <CheckIcon className="size-3" strokeWidth={3} /> : position}
    </span>
);

const JourneySteps = ({className, orientation = 'horizontal', steps, ...props}: JourneyStepsProps) => (
    <ol className={twMerge(listStyles[orientation], className)} {...props}>
        {steps.map((step, index) => (
            <li
                aria-current={step.status === 'current' ? 'step' : undefined}
                className={itemStyles[orientation]}
                key={step.id}
            >
                {orientation === 'horizontal' && index > 0 && (
                    <span aria-hidden="true" className="mx-2 h-px w-4 bg-stroke-neutral-secondary" />
                )}

                <JourneyStepMarker position={index + 1} status={step.status} />

                <span className="flex min-w-0 flex-col">
                    <span className={labelStyles[step.status]}>
                        {step.status === 'complete' && <span className="sr-only">Completed:</span>}

                        {step.label}
                    </span>

                    {step.description && (
                        <span className="text-xs text-content-neutral-secondary">{step.description}</span>
                    )}
                </span>

                {step.action}
            </li>
        ))}
    </ol>
);

JourneySteps.displayName = 'JourneySteps';

export default JourneySteps;
export type {JourneyStepI, JourneyStepsProps, JourneyStepStatusType};
