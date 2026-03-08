import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CircleIcon, LoaderCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface LoadingIndicatorProps {
    isFetching: number;
    isOnline: boolean;
}

const LoadingIndicator = ({isFetching, isOnline}: LoadingIndicatorProps) => {
    const isSaving = isFetching > 0;

    return (
        <Tooltip>
            <TooltipTrigger
                aria-label="Loading indicator"
                className="inline-flex size-9 cursor-pointer items-center justify-center rounded-md hover:bg-surface-neutral-primary-hover focus:outline focus:outline-ring"
            >
                {isOnline && isSaving ? (
                    <LoaderCircleIcon className="size-3 animate-spin text-content-warning" />
                ) : (
                    <CircleIcon
                        className={twMerge(
                            'size-3 cursor-pointer fill-content-destructive text-content-destructive',
                            isOnline && !isSaving && 'fill-content-success text-content-success'
                        )}
                    />
                )}
            </TooltipTrigger>

            <TooltipContent>
                {isOnline ? <>{!isSaving ? 'All changes are saved' : 'Saving your progress'}</> : 'You are offline'}
            </TooltipContent>
        </Tooltip>
    );
};

export default LoadingIndicator;
