import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CircleIcon, LoaderCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface LoadingIndicatorProps {
    className?: string;
    isFetching: number;
    isOnline: boolean;
    showLabel?: boolean;
}

const LoadingIndicator = ({className, isFetching, isOnline, showLabel = false}: LoadingIndicatorProps) => {
    const isSaving = isOnline && !!isFetching;

    return (
        <Tooltip>
            <TooltipTrigger
                aria-label="Loading indicator"
                className={twMerge(
                    'inline-flex size-9 cursor-pointer items-center justify-center rounded-md hover:bg-surface-neutral-primary-hover focus:outline-ring focus:outline-solid',
                    showLabel &&
                        'h-7 w-auto gap-1.5 rounded-full px-2.5 text-xs font-semibold hover:bg-surface-neutral-primary-hover',
                    showLabel && isSaving && 'bg-surface-warning-secondary text-content-warning',
                    showLabel && !isOnline && 'bg-surface-destructive-secondary text-content-destructive',
                    className
                )}
            >
                {isSaving ? (
                    <LoaderCircleIcon className="size-4 animate-spin text-content-warning" />
                ) : (
                    <CircleIcon
                        className={twMerge(
                            'size-4 cursor-pointer fill-content-destructive text-content-destructive',
                            isOnline && !isFetching && 'fill-content-success text-content-success'
                        )}
                    />
                )}

                {showLabel && (isSaving || !isOnline) && <span>{isSaving ? 'Saving…' : 'Offline'}</span>}
            </TooltipTrigger>

            <TooltipContent>
                {isOnline ? <>{!isFetching ? 'All changes are saved' : 'Saving your progress'}</> : 'You are offline'}
            </TooltipContent>
        </Tooltip>
    );
};

export default LoadingIndicator;
