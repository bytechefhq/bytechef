import {
    CheckCircleIcon,
    ExclamationTriangleIcon,
    InformationCircleIcon,
    XCircleIcon,
} from '@heroicons/react/24/outline';
import React, {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

export interface AlertProps {
    type?: 'error' | 'info' | 'success' | 'warning';
    text: ReactNode;
}

const Alert = ({type = 'info', text}: AlertProps) => (
    <div
        className={twMerge(
            'my-2 rounded-md p-4',
            type === 'error' && 'bg-red-50',
            type === 'info' && 'bg-blue-50',
            type === 'success' && 'bg-green-50',
            type === 'warning' && 'bg-yellow-50'
        )}
    >
        <div className="flex items-center">
            <div className="shrink-0">
                {type === 'error' && (
                    <XCircleIcon
                        className="h-5 w-5 text-blue-400"
                        aria-hidden="true"
                    />
                )}

                {type === 'info' && (
                    <InformationCircleIcon
                        className="h-5 w-5 text-blue-400"
                        aria-hidden="true"
                    />
                )}

                {type === 'success' && (
                    <CheckCircleIcon
                        className="h-5 w-5 text-blue-400"
                        aria-hidden="true"
                    />
                )}

                {type === 'warning' && (
                    <ExclamationTriangleIcon
                        className="h-5 w-5 text-blue-400"
                        aria-hidden="true"
                    />
                )}
            </div>

            <div className="ml-3 flex-1 md:flex md:justify-between">
                <p
                    className={twMerge(
                        'text-sm',
                        type === 'error' && 'text-red-700',
                        type === 'info' && 'text-blue-700',
                        type === 'success' && 'text-green-700',
                        type === 'warning' && 'text-yellow-700'
                    )}
                >
                    {text}
                </p>
            </div>
        </div>
    </div>
);

export default Alert;
