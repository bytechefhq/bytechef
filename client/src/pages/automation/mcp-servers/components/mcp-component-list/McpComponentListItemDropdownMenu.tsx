import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {
    McpComponent,
    useDeleteEmbeddedMcpComponentMutation,
    useDeleteMcpComponentMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface McpComponentListItemDropDownProps {
    embedded?: boolean;
    mcpComponent: McpComponent;
    onEditClick: () => void;
}

const McpComponentListItemDropdownMenu = ({embedded, mcpComponent, onEditClick}: McpComponentListItemDropDownProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditWorkspaceMcpServer = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');
    const canEditEmbeddedMcpServer = useHasEmbeddedScope('MCP_EDIT');

    const queryClient = useQueryClient();

    const canEditMcpServer = embedded ? canEditEmbeddedMcpServer : canEditWorkspaceMcpServer;

    const handleDeleteSuccess = () => {
        void queryClient.invalidateQueries({
            queryKey: [embedded ? 'embeddedMcpComponentsByServerId' : 'mcpComponentsByServerId'],
        });

        if (!embedded) {
            void queryClient.invalidateQueries({
                queryKey: ['workspaceMcpServers'],
            });
        }

        setShowDeleteDialog(false);
    };

    const deleteEmbeddedMcpComponentMutation = useDeleteEmbeddedMcpComponentMutation({
        onSuccess: handleDeleteSuccess,
    });

    const deleteMcpComponentMutation = useDeleteMcpComponentMutation({
        onSuccess: handleDeleteSuccess,
    });

    const isDeletePending = embedded
        ? deleteEmbeddedMcpComponentMutation.isPending
        : deleteMcpComponentMutation.isPending;

    const handleConfirmDelete = () => {
        if (embedded) {
            deleteEmbeddedMcpComponentMutation.mutate({id: mcpComponent.id.toString()});
        } else {
            deleteMcpComponentMutation.mutate({id: mcpComponent.id.toString()});
        }
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
                    <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

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

export default McpComponentListItemDropdownMenu;
