import {Content, Root, Trigger} from '@radix-ui/react-hover-card';
import {PropsWithChildren, ReactNode} from 'react';

export interface HoverCardProps {
    text: ReactNode;
}

const HoverCard = ({children, text}: PropsWithChildren<HoverCardProps>) => (
    <Root>
        <Trigger asChild>{children}</Trigger>

        <Content
            align="center"
            className="max-w-md rounded-lg bg-white p-4 shadow-lg dark:bg-gray-800 md:w-full"
            sideOffset={4}
        >
            <div className="flex h-full w-full space-x-4">
                <p className="mt-1 text-sm font-normal text-gray-700 dark:text-gray-400">
                    {text}
                </p>
            </div>
        </Content>
    </Root>
);

export default HoverCard;
