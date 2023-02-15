import React, {ReactNode} from 'react';
import {Arrow, Content, Root, Trigger} from '@radix-ui/react-popover';
import ContextualMenu from '../nodes/ContextualMenu';
import {useGetComponentDefinitionsQuery} from '../../../queries/componentDefinitions';
import {useGetTaskDispatcherDefinitionsQuery} from '../../../queries/taskDispatcherDefinitions';

interface PopoverMenuProps {
    children: ReactNode;
    id?: string;
}

const PopoverMenu = ({children, id}: PopoverMenuProps) => {
    const {data: components} = useGetComponentDefinitionsQuery();

    const {data: flowControls} = useGetTaskDispatcherDefinitionsQuery();

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
