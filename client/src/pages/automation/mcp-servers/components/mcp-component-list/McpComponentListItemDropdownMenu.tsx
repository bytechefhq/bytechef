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

    const queryClient = useQueryClient();

    const handleDeleteSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: [embedded ? 'embeddedMcpComponentsByServerId' : 'mcpComponentsByServerId'],
        });
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

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="relative z-10" icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
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
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpComponentListItemDropdownMenu;
