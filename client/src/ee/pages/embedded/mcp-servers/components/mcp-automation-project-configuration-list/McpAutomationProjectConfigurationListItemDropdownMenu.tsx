import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

import {McpAutomationProjectConfigurationItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationListItemDropdownMenu from './hooks/useMcpAutomationProjectConfigurationListItemDropdownMenu';

interface McpAutomationProjectConfigurationListItemDropdownMenuProps {
    mcpAutomationProjectConfiguration: McpAutomationProjectConfigurationItemType;
    onEditWorkflowsClick: () => void;
}

const McpAutomationProjectConfigurationListItemDropdownMenu = ({
    mcpAutomationProjectConfiguration,
    onEditWorkflowsClick,
}: McpAutomationProjectConfigurationListItemDropdownMenuProps) => {
    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpAutomationProjectConfigurationListItemDropdownMenu(mcpAutomationProjectConfiguration.id);

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="relative z-10" icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem icon={<EditIcon />} label="Edit Workflows" onClick={onEditWorkflowsClick} />

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

export default McpAutomationProjectConfigurationListItemDropdownMenu;
