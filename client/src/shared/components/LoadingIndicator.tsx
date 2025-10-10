import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CircleIcon, LoaderCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

const LoadingIndicator = ({isFetching, isOnline}: {isFetching: number; isOnline: boolean}) => {
    return (
        <Tooltip>
            <TooltipTrigger
                aria-label="Loader notification indicator"
                className="inline-flex size-9 cursor-pointer items-center justify-center rounded-md hover:bg-surface-neutral-primary-hover focus:outline focus:outline-ring"
            >
                {isOnline && isFetching ? (
                    <LoaderCircleIcon className="size-3 animate-spin text-content-warning" />
                ) : (
                    <CircleIcon
                        className={twMerge(
                            'size-3 cursor-pointer fill-content-destructive text-content-destructive',
                            isOnline && !isFetching && 'fill-content-success text-content-success'
                        )}
                    />
                )}
            </TooltipTrigger>

            <TooltipContent>
                {isOnline ? <>{!isFetching ? 'All changes are saved' : 'Saving your progress'}</> : 'You are offline'}
            </TooltipContent>
        </Tooltip>
    );
};

export default LoadingIndicator;
