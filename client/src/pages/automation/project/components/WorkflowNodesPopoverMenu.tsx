import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {PropsWithChildren} from 'react';

import WorkflowNodesPopoverMenuList from './WorkflowNodesPopoverMenuList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    id?: string;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
}

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
            className="w-workflow-nodes-popover-menu-width p-0 will-change-auto"
            side="right"
            sideOffset={4}
        >
            {id && (
                <WorkflowNodesPopoverMenuList
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
