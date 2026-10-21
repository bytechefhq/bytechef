import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
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
    const canDeleteConfiguration = useHasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_DELETE');
    const canEditConfiguration = useHasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_EDIT');

    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpIntegrationInstanceConfigurationListItemDropdownMenu(mcpIntegrationInstanceConfiguration.id.toString());

    if (!canEditConfiguration && !canDeleteConfiguration) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="MCP Integration Actions"
                        className="relative z-10"
                        icon={<EllipsisVerticalIcon />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    {canEditConfiguration && (
                        <>
                            <DropdownMenuItem
                                icon={<EditIcon />}
                                label="Edit Workflows"
                                onClick={onEditWorkflowsClick}
                            />

                            <DropdownMenuItem
                                icon={<RefreshCwIcon />}
                                label="Update Integration Version"
                                onClick={onUpdateIntegrationVersionClick}
                            />
                        </>
                    )}

                    {canEditConfiguration && canDeleteConfiguration && <DropdownMenuSeparator />}

                    {canDeleteConfiguration && (
                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={() => setShowDeleteDialog(true)}
                            variant="destructive"
                        />
                    )}
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
