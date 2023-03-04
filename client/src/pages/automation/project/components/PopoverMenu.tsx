import React, {ReactNode} from 'react';
import {Arrow, Content, Portal, Root, Trigger} from '@radix-ui/react-popover';
import ContextualMenu from '../nodes/ContextualMenu';
import {useGetComponentDefinitionsQuery} from '../../../../queries/componentDefinitions';
import {useGetTaskDispatcherDefinitionsQuery} from '../../../../queries/taskDispatcherDefinitions';

interface PopoverMenuProps {
    children: ReactNode;
    id?: string;
    edge?: boolean;
}

const PopoverMenu = ({children, id, edge = false}: PopoverMenuProps) => {
    const {data: components} = useGetComponentDefinitionsQuery();

    const {data: flowControls} = useGetTaskDispatcherDefinitionsQuery();

    return (
        <Root>
            <Trigger asChild>{children}</Trigger>

            <Portal>
                <Content
                    align="start"
                    sideOffset={4}
                    side="right"
                    // eslint-disable-next-line tailwindcss/no-custom-classname
                    className="relative w-96 animate-slide-down rounded-lg bg-white shadow-md will-change-auto dark:bg-gray-800"
                >
                    <Arrow className="fill-current text-white dark:text-gray-800" />

                    {id && (
                        <ContextualMenu
                            id={id}
                            components={components}
                            flowControls={flowControls}
                            edge={edge}
                        />
                    )}
                </Content>
            </Portal>
        </Root>
    );
};

export default PopoverMenu;
