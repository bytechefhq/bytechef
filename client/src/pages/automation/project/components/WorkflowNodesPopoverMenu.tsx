import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {PropsWithChildren} from 'react';

import WorkflowNodesList from './WorkflowNodesList';

type WorkflowNodesPopoverMenuProps = {
    id?: string;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
} & PropsWithChildren;

const WorkflowNodesPopoverMenu = ({
    children,
    edge = false,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    id,
}: WorkflowNodesPopoverMenuProps) => (
    <Popover>
        <PopoverTrigger asChild>{children}</PopoverTrigger>

        <PopoverContent
            align="start"
            className="w-[500px] p-0 will-change-auto"
            side="right"
            sideOffset={4}
        >
            {id && (
                <WorkflowNodesList
                    edge={edge}
                    hideActionComponents={hideActionComponents}
                    hideTaskDispatchers={hideTaskDispatchers}
                    hideTriggerComponents={hideTriggerComponents}
                    id={id}
                />
            )}
        </PopoverContent>
    </Popover>
);

export default WorkflowNodesPopoverMenu;
