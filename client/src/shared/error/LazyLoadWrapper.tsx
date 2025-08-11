import {Skeleton} from '@/components/ui/skeleton';
import {FC, ReactNode, Suspense} from 'react';
import {twMerge} from 'tailwind-merge';

import ErrorBoundary from './ErrorBoundary';

interface LazyLoadWrapperProps {
    children: ReactNode;
    errorFallback?: ReactNode;
    fallback?: ReactNode;
    hasLeftSidebar?: boolean;
    onReset?: () => void;
}

/**
 * A component that combines ErrorBoundary and Suspense to handle both
 * lazy loading states and errors in one wrapper.
 */
const LazyLoadWrapper: FC<LazyLoadWrapperProps> = ({children, errorFallback, fallback, hasLeftSidebar, onReset}) => {
    const defaultFallback = (
        <div className={twMerge('flex size-full items-center', !hasLeftSidebar && 'justify-center p-8')}>
            {hasLeftSidebar && (
                <aside className="hidden h-full gap-2 border-r border-r-border/50 bg-muted/50 p-4 lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
                    <Skeleton className="h-6 w-full" />

                    <Skeleton className="mt-4 h-4 w-1/2" />

                    {Array.from({length: 6}).map((_, index) => (
                        <Skeleton className="h-6 w-full" key={index} />
                    ))}

                    <Skeleton className="mt-4 h-4 w-1/2" />

                    {Array.from({length: 6}).map((_, index) => (
                        <Skeleton className="h-6 w-full" key={index} />
                    ))}
                </aside>
            )}

            <div className={twMerge('flex animate-pulse space-x-2', hasLeftSidebar && 'mx-auto')}>
                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>
            </div>
        </div>
    );

    return (
        <ErrorBoundary fallback={errorFallback} onReset={onReset}>
            <Suspense fallback={fallback || defaultFallback}>{children}</Suspense>
        </ErrorBoundary>
    );
};

export default LazyLoadWrapper;
