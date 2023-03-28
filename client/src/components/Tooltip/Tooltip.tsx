import {Side} from '@radix-ui/react-popper';
import {Arrow, Content, Provider, Root, Trigger} from '@radix-ui/react-tooltip';
import {PropsWithChildren} from 'react';

interface TooltipProps {
    side?: Side;
    text: string;
}

const Tooltip = ({side, text, children}: PropsWithChildren<TooltipProps>) => {
    return (
        <Provider>
            <Root>
                <Trigger asChild>{children}</Trigger>

                <Content
                    className="z-10 inline-flex items-center rounded-md bg-gray-800 px-4 py-2.5 radix-side-bottom:animate-slide-up-fade radix-side-left:animate-slide-right-fade radix-side-right:animate-slide-left-fade radix-side-top:animate-slide-down-fade dark:bg-gray-800"
                    side={side}
                    sideOffset={4}
                >
                    <Arrow className="fill-current text-gray-800 dark:text-gray-800" />

                    <span className="block text-xs leading-none text-white dark:text-gray-100">
                        {text}
                    </span>
                </Content>
            </Root>
        </Provider>
    );
};

export {Tooltip};
