import React, {ReactNode} from 'react';
import {Arrow, Content, Root, Trigger} from '@radix-ui/react-popover';
import {
    useGetComponentsQuery,
    useGetFlowControlsQuery,
} from 'queries/integration.queries';
import ContextualMenu from '../nodes/ContextualMenu';

interface PopoverMenuProps {
    children: ReactNode;
    id?: string;
}

const PopoverMenu = ({children, id}: PopoverMenuProps) => {
    const {data: components} = useGetComponentsQuery();

    const {data: flowControls} = useGetFlowControlsQuery();

    return (
        <div className="relative inline-block text-left">
            <Root>
                <Trigger asChild>{children}</Trigger>

                <Content
                    align="start"
                    sideOffset={4}
                    side="right"
                    // eslint-disable-next-line tailwindcss/no-custom-classname
                    className="nowheel relative w-96 animate-slide-down rounded-lg bg-white shadow-md will-change-auto dark:bg-gray-800"
                >
                    <Arrow className="fill-current text-white dark:text-gray-800" />

                    {id && (
                        <ContextualMenu
                            id={id}
                            components={components}
                            flowControls={flowControls}
                        />
                    )}
                </Content>
            </Root>
        </div>
    );
};

export default PopoverMenu;
