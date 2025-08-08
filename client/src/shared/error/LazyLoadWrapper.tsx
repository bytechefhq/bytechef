import {FC, ReactNode, Suspense} from 'react';

import ErrorBoundary from './ErrorBoundary';

interface LazyLoadWrapperProps {
    children: ReactNode;
    fallback?: ReactNode;
    errorFallback?: ReactNode;
    onReset?: () => void;
}

/**
 * A component that combines ErrorBoundary and Suspense to handle both
 * lazy loading states and errors in one wrapper.
 */
const LazyLoadWrapper: FC<LazyLoadWrapperProps> = ({children, errorFallback, fallback, onReset}) => {
    const defaultFallback = (
        <div className="flex size-full items-center justify-center p-8">
            <div className="flex animate-pulse space-x-2">
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
