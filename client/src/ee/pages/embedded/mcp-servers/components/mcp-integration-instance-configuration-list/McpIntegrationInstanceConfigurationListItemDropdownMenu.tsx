import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {McpIntegrationInstanceConfiguration} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon} from 'lucide-react';

import useMcpIntegrationInstanceConfigurationListItemDropdownMenu from './hooks/useMcpIntegrationInstanceConfigurationListItemDropdownMenu';

interface McpIntegrationInstanceConfigurationListItemDropdownMenuProps {
    mcpIntegrationInstanceConfiguration: McpIntegrationInstanceConfiguration;
    onEditWorkflowsClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const McpIntegrationInstanceConfigurationListItemDropdownMenu = ({
    mcpIntegrationInstanceConfiguration,
    onEditWorkflowsClick,
    onUpdateIntegrationVersionClick,
}: McpIntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} =
        useMcpIntegrationInstanceConfigurationListItemDropdownMenu(mcpIntegrationInstanceConfiguration.id.toString());

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onEditWorkflowsClick}>Edit Workflows</DropdownMenuItem>

                    <DropdownMenuItem onClick={onUpdateIntegrationVersionClick}>
                        Update Integration Version
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                        <span className="w-full">Delete</span>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpIntegrationInstanceConfigurationListItemDropdownMenu;
