import React from 'react';
import * as HoverCardPrimitive from '@radix-ui/react-hover-card';
import {clsx} from 'clsx';

export const Name: React.FC<{name: string; description: string}> = ({
    name,
    description,
}) => {
    return (
        <HoverCardPrimitive.Root>
            <HoverCardPrimitive.Trigger asChild>
                <span
                    className={clsx(
                        'items-center justify-center rounded-full bg-white dark:bg-gray-900'
                    )}
                >
                    {name}
                </span>
            </HoverCardPrimitive.Trigger>

            <HoverCardPrimitive.Content
                align="center"
                sideOffset={4}
                className={clsx(
                    'max-w-md rounded-lg p-4 md:w-full',
                    'bg-white dark:bg-gray-800',
                    'border'
                )}
            >
                <div className="flex h-full w-full space-x-4">
                    <div>
                        <p className="mt-1 text-sm font-normal text-gray-700 dark:text-gray-400">
                            {description}
                        </p>
                    </div>
                </div>
            </HoverCardPrimitive.Content>
        </HoverCardPrimitive.Root>
    );
};
