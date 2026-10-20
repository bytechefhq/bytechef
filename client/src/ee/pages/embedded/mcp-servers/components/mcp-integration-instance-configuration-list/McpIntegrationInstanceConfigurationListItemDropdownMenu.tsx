import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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
                    <DropdownMenuItem icon={<EditIcon />} label="Edit Workflows" onClick={onEditWorkflowsClick} />

                    <DropdownMenuItem
                        icon={<RefreshCwIcon />}
                        label="Update Integration Version"
                        onClick={onUpdateIntegrationVersionClick}
                    />

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    />
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
