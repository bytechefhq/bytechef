import {Side} from '@radix-ui/react-popper';
import {Arrow, Content, Provider, Root, Trigger} from '@radix-ui/react-tooltip';
import {PropsWithChildren} from 'react';

interface TooltipProps {
    text: string;
    side?: Side;
}

const Tooltip = ({children, side, text}: PropsWithChildren<TooltipProps>) => (
    <Provider>
        <Root>
            <Trigger asChild>{children}</Trigger>

            <Content
                className="radix-side-bottom:animate-slide-up-fade radix-side-left:animate-slide-right-fade radix-side-right:animate-slide-left-fade radix-side-top:animate-slide-down-fade z-10 inline-flex max-w-sm items-center rounded-md bg-gray-800 px-4 py-2.5"
                side={side}
                sideOffset={4}
            >
                <Arrow className="fill-current text-gray-800" />

                <span className="block text-xs leading-4 text-white">
                    {text}
                </span>
            </Content>
        </Root>
    </Provider>
);

export default Tooltip;
