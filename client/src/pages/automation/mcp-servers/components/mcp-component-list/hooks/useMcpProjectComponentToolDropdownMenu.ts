import {McpTool, useDeleteEmbeddedMcpToolMutation, useDeleteMcpToolMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseMcpProjectComponentToolDropdownMenuProps {
    embedded?: boolean;
    mcpTool: McpTool;
}

export default function useMcpProjectComponentToolDropdownMenu({
    embedded,
    mcpTool,
}: UseMcpProjectComponentToolDropdownMenuProps) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const handleDeleteSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: [embedded ? 'embeddedMcpComponentsByServerId' : 'mcpComponentsByServerId'],
        });

        setShowDeleteDialog(false);
    };

    const deleteEmbeddedMcpToolMutation = useDeleteEmbeddedMcpToolMutation({
        onSuccess: handleDeleteSuccess,
    });

    const deleteMcpToolMutation = useDeleteMcpToolMutation({
        onSuccess: handleDeleteSuccess,
    });

    const handleConfirmDelete = () => {
        if (embedded) {
            deleteEmbeddedMcpToolMutation.mutate({id: mcpTool.id});
        } else {
            deleteMcpToolMutation.mutate({id: mcpTool.id});
        }
    };

    return {
        handleConfirmDelete,
        isDeletePending: embedded ? deleteEmbeddedMcpToolMutation.isPending : deleteMcpToolMutation.isPending,
        setShowDeleteDialog,
        showDeleteDialog,
    };
}
