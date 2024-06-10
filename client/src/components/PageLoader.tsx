import {CrossCircledIcon} from '@radix-ui/react-icons';
import {PropsWithChildren} from 'react';

type TErrorType = Error | null;

const PageLoader = ({
    children,
    errors = [],
    loading,
}: PropsWithChildren<{errors?: Array<TErrorType>; loading: boolean}>) => {
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
                            <CrossCircledIcon aria-hidden="true" className="mr-1 size-5 text-red-400" />

                            <h1>Some error occurred.</h1>
                        </div>
                    ) : (
                        <div className="flex animate-pulse space-x-2">
                            <div className="size-3 rounded-full bg-gray-500"></div>

                            <div className="size-3 rounded-full bg-gray-500"></div>

                            <div className="size-3 rounded-full bg-gray-500"></div>
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
