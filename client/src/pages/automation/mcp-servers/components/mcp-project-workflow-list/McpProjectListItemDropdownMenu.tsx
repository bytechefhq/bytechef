import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {McpProject, useDeleteMcpProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

import McpProjectListItemAlertDialog from './McpProjectListItemAlertDialog';

interface McpProjectListItemDropdownMenuProps {
    mcpProject: McpProject;
    onUpdateProjectVersionClick: () => void;
}

const McpProjectListItemDropdownMenu = ({
    mcpProject,
    onUpdateProjectVersionClick,
}: McpProjectListItemDropdownMenuProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const queryClient = useQueryClient();

    const deleteMcpProjectMutation = useDeleteMcpProjectMutation({
        onError: () => {
            setIsPending(false);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });
            setShowDeleteDialog(false);
            setIsPending(false);
        },
    });

    const handleDeleteClick = () => {
        setShowDeleteDialog(true);
    };

    const handleConfirmDelete = async () => {
        setIsPending(true);
        try {
            await deleteMcpProjectMutation.mutateAsync({
                id: mcpProject.id.toString(),
            });
        } catch (error) {
            console.error('Error deleting MCP project:', error);
        }
    };

    const handleCancelDelete = () => {
        setShowDeleteDialog(false);
    };

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onUpdateProjectVersionClick}>Update Project Version</DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={handleDeleteClick}>
                        <span className="w-full">Delete</span>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            {showDeleteDialog && (
                <McpProjectListItemAlertDialog
                    isPending={isPending}
                    onCancelClick={handleCancelDelete}
                    onDeleteClick={handleConfirmDelete}
                />
            )}
        </>
    );
};

export default McpProjectListItemDropdownMenu;
