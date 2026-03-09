import {McpTool, useDeleteMcpToolMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseMcpComponentToolDropdownMenuProps {
    mcpTool: McpTool;
}

export default function useMcpComponentToolDropdownMenu({mcpTool}: UseMcpComponentToolDropdownMenuProps) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpToolMutation = useDeleteMcpToolMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });

            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpToolMutation.mutate({
            id: mcpTool.id,
        });
    };

    return {
        handleConfirmDelete,
        setShowDeleteDialog,
        showDeleteDialog,
    };
}
