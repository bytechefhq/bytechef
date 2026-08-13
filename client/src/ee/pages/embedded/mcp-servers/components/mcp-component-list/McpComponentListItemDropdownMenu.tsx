import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {McpComponent, useDeleteMcpComponentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface McpComponentListItemDropDownProps {
    mcpComponent: McpComponent;
    onEditClick: () => void;
}

const McpComponentListItemDropdownMenu = ({mcpComponent, onEditClick}: McpComponentListItemDropDownProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpComponentMutation = useDeleteMcpComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });
            queryClient.invalidateQueries({
                queryKey: ['embeddedMcpServers'],
            });
            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpComponentMutation.mutate({
            id: mcpComponent.id.toString(),
        });
    };

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onEditClick}>
                        <span className="w-full">Edit</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem onClick={() => setShowDeleteDialog(true)} variant="destructive">
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

export default McpComponentListItemDropdownMenu;
