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
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {McpComponent, useDeleteMcpComponentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface McpComponentListItemDropDownProps {
    mcpComponent: McpComponent;
    onEditClick: () => void;
}

const McpComponentListItemDropdownMenu = ({mcpComponent, onEditClick}: McpComponentListItemDropDownProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditMcpServer = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');

    const queryClient = useQueryClient();

    const deleteMcpComponentMutation = useDeleteMcpComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });
            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpComponentMutation.mutate({
            id: mcpComponent.id.toString(),
        });
    };

    if (!canEditMcpServer) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="MCP Component Actions"
                        className="relative z-10"
                        icon={<EllipsisVerticalIcon />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                        <EditIcon /> Edit
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
                isPending={deleteMcpComponentMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpComponentListItemDropdownMenu;
