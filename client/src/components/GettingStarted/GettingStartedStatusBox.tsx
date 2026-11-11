import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface GettingStartedStatusBoxProps {
    children?: ReactNode;
    'data-testid'?: string;
    indicatorTestId?: string;
    title: string;
    tone: 'error' | 'idle' | 'success' | 'waiting';
}

const DOT_CLASS_NAMES: Record<GettingStartedStatusBoxProps['tone'], string> = {
    error: 'bg-surface-destructive-primary',
    idle: 'bg-content-neutral-tertiary',
    success: 'bg-surface-success-primary',
    waiting: 'animate-pulse bg-surface-success-primary',
};

const GettingStartedStatusBox = ({
    children,
    'data-testid': dataTestId,
    indicatorTestId,
    title,
    tone,
}: GettingStartedStatusBoxProps) => (
    <div
        className="flex items-center gap-2 rounded-md border border-stroke-neutral-secondary p-3 text-sm"
        data-testid={dataTestId}
    >
        <span
            aria-hidden="true"
            className={twMerge('size-2 shrink-0 rounded-full', DOT_CLASS_NAMES[tone])}
            data-testid={indicatorTestId}
        />

        <div className="min-w-0 flex-1 space-y-1">
            <div className="font-medium text-content-neutral-primary">{title}</div>

            {children}
        </div>
    </div>
);

export default GettingStartedStatusBox;
