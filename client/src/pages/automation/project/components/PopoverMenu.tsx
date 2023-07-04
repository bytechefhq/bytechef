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
                    className="w-112 animate-slide-down relative z-[9999] rounded-lg bg-white shadow-md will-change-auto"
                >
                    <Arrow className="fill-current text-white" />

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
