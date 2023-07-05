import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {ReactNode} from 'react';

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
        <Popover>
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent
                align="start"
                className="w-[500px] p-0 will-change-auto"
                sideOffset={4}
                side="right"
            >
                {id && (
                    <ContextualMenu
                        id={id}
                        components={components}
                        flowControls={flowControls}
                        edge={edge}
                    />
                )}
            </PopoverContent>
        </Popover>
        // <Root>
        //     <Trigger asChild>{children}</Trigger>
        //
        //     <Portal>
        //         <Content
        //             align="start"
        //             sideOffset={4}
        //             side="right"
        //             className="w-112 animate-slide-down relative z-[9999] rounded-lg bg-white shadow-md will-change-auto"
        //         >
        //
        //         </Content>
        //     </Portal>
        // </Root>
    );
};

export default PopoverMenu;
