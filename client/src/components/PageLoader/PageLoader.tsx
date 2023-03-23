import {XCircleIcon} from '@heroicons/react/24/outline';
import React, {PropsWithChildren} from 'react';

type TError = Error | null;

const PageLoader = ({
    children,
    errors,
    loading,
}: PropsWithChildren<{errors: TError[]; loading: boolean}>): JSX.Element => {
    let errorExists = false;

    errors.forEach((error) => {
        if (error) {
            errorExists = true;
        }
    });

    return (
        <>
            {errorExists || loading ? (
                <div className="flex min-h-screen min-w-full items-center justify-center p-5">
                    {errorExists ? (
                        <div className="flex items-center text-red-700">
                            <XCircleIcon
                                className="mr-1 h-5 w-5 text-red-400"
                                aria-hidden="true"
                            />

                            <h1>Some error occurred.</h1>
                        </div>
                    ) : (
                        <div className="flex animate-pulse space-x-2">
                            <div className="h-3 w-3 rounded-full bg-gray-500"></div>

                            <div className="h-3 w-3 rounded-full bg-gray-500"></div>

                            <div className="h-3 w-3 rounded-full bg-gray-500"></div>
                        </div>
                    )}
                </div>
            ) : (
                children
            )}
        </>
    );
};

export default PageLoader;
