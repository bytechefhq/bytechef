import React from 'react';
import {Root, Trigger, Content} from '@radix-ui/react-hover-card';

export const Name: React.FC<{name: string; description: string}> = ({
    name,
    description,
}) => {
    return (
        <Root>
            <Trigger asChild>
                <span className="text-lg font-semibold text-gray-700">
                    {name}
                </span>
            </Trigger>

            <Content
                align="center"
                sideOffset={4}
                className="max-w-md rounded-lg bg-white p-4 shadow-lg dark:bg-gray-800 md:w-full"
            >
                <div className="flex h-full w-full space-x-4">
                    <p className="mt-1 text-sm font-normal text-gray-700 dark:text-gray-400">
                        {description}
                    </p>
                </div>
            </Content>
        </Root>
    );
};
