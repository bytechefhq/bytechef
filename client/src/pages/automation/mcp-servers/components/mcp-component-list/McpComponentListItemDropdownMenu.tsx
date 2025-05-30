import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {McpComponentType} from '@/shared/queries/platform/mcpComponents.queries';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {EllipsisVerticalIcon} from 'lucide-react';

import McpComponentDialog from '../McpComponentDialog';

interface McpComponentListItemDropDownProps {
    mcpComponent: McpComponentType;
    mcpServer: McpServerType;
}

const McpComponentListItemDropdownMenu = ({mcpComponent, mcpServer}: McpComponentListItemDropDownProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem>
                    <McpComponentDialog
                        mcpComponent={mcpComponent}
                        mcpServerId={mcpServer.id}
                        triggerNode={<span className="w-full">Edit</span>}
                    />
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpComponentListItemDropdownMenu;
