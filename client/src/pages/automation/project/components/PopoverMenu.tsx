import {Arrow, Content, Portal, Root, Trigger} from '@radix-ui/react-popover';
import {ReactNode} from 'react';

import {useGetComponentDefinitionsQuery} from '../../../../queries/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '../../../../queries/taskDispatcherDefinitions.queries';
import ContextualMenu from '../components/ContextualMenu';

interface PopoverMenuProps {
    children: ReactNode;
    id?: string;
    edge?: boolean;
}

const PopoverMenu = ({children, edge = false, id}: PopoverMenuProps) => {
    const {data: components} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
    });

    const {data: flowControls} = useGetTaskDispatcherDefinitionsQuery();

    return (
        <Root>
            <Trigger asChild>{children}</Trigger>

            <Portal>
                <Content
                    align="start"
                    sideOffset={4}
                    side="right"
                    className="relative z-[9999] w-112 animate-slide-down rounded-lg bg-white shadow-md will-change-auto dark:bg-gray-800"
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
