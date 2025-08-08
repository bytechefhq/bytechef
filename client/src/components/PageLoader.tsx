import {CrossCircledIcon} from '@radix-ui/react-icons';
import {PropsWithChildren} from 'react';
import {twMerge} from 'tailwind-merge';

import LoadingDots from './LoadingDots';

type TErrorType = Error | null;

const PageLoader = ({
    children,
    className,
    errors = [],
    loading,
}: PropsWithChildren<{errors?: Array<TErrorType | unknown>; className?: string; loading: boolean}>) => {
    let errorExists = false;

    errors.forEach((error) => {
        if (error) {
            errorExists = true;
        }
    });

    return (
        <>
            {errorExists || loading ? (
                <div className={twMerge('flex min-h-screen min-w-full items-center justify-center p-5', className)}>
                    {errorExists ? (
                        <div className="flex items-center text-red-700">
                            <CrossCircledIcon aria-hidden="true" className="mr-1 size-5 text-red-400" />

                            <h1>Some error occurred.</h1>
                        </div>
                    ) : (
                        <LoadingDots />
                    )}
                </div>
            ) : (
                children
            )}
        </>
    );
};

export default PageLoader;
