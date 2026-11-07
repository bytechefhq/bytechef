import {TooltipTrigger} from '@/components/ui/tooltip';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface TooltipTriggerIconPropsI {
    children: ReactNode;
    className?: string;
    label: string;
}

const TooltipTriggerIcon = ({children, className, label}: TooltipTriggerIconPropsI) => (
    <TooltipTrigger asChild>
        <button
            aria-label={label}
            className={twMerge(
                'inline-flex shrink-0 cursor-help items-center justify-center rounded-sm text-inherit',
                'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring',
                className
            )}
            type="button"
        >
            {children}
        </button>
    </TooltipTrigger>
);

export default TooltipTriggerIcon;
