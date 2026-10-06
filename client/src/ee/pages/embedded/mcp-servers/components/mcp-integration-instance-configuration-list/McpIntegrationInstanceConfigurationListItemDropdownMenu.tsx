import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EditIcon, EllipsisVerticalIcon, RefreshCwIcon, Trash2Icon} from 'lucide-react';

import {McpIntegrationInstanceConfigurationItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationListItemDropdownMenu from './hooks/useMcpIntegrationInstanceConfigurationListItemDropdownMenu';

interface McpIntegrationInstanceConfigurationListItemDropdownMenuProps {
    mcpIntegrationInstanceConfiguration: McpIntegrationInstanceConfigurationItemType;
    onEditWorkflowsClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const McpIntegrationInstanceConfigurationListItemDropdownMenu = ({
    mcpIntegrationInstanceConfiguration,
    onEditWorkflowsClick,
    onUpdateIntegrationVersionClick,
}: McpIntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpIntegrationInstanceConfigurationListItemDropdownMenu(mcpIntegrationInstanceConfiguration.id.toString());

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="relative z-10" icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditWorkflowsClick}>
                        <EditIcon /> Edit Workflows
                    </DropdownMenuItem>

                    <DropdownMenuItem className="dropdown-menu-item" onClick={onUpdateIntegrationVersionClick}>
                        <RefreshCwIcon /> Update Integration Version
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    >
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpIntegrationInstanceConfigurationListItemDropdownMenu;
