import {
    McpServer,
    useDeleteWorkspaceMcpServerMutation,
    useUpdateMcpServerMutation,
    useUpdateMcpServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpServerListItem = (mcpServer: McpServer) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const updateMcpServerMutation = useUpdateMcpServerMutation();
    const deleteWorkspaceMcpServerMutation = useDeleteWorkspaceMcpServerMutation();
    const updateMcpServerTagsMutation = useUpdateMcpServerTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
            queryClient.invalidateQueries({queryKey: ['mcpServerTags']});
        },
    });

    const handleOnCheckedChange = async (value: boolean) => {
        setIsEnablePending(true);

        updateMcpServerMutation.mutate(
            {
                id: mcpServer.id,
                input: {
                    enabled: value,
                },
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                    setIsEnablePending(false);
                },
            }
        );
    };

    const handleDeleteClick = async () => {
        setIsPending(true);

        deleteWorkspaceMcpServerMutation.mutate(
            {
                id: mcpServer.id,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                    setShowDeleteDialog(false);
                },
            }
        );
    };

    return {
        handleDeleteClick,
        handleOnCheckedChange,
        isEnablePending,
        isPending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        setShowMcpComponentDialog,
        setShowWorkflowDialog,
        showDeleteDialog,
        showEditDialog,
        showMcpComponentDialog,
        showWorkflowDialog,
        updateMcpServerTagsMutation,
    };
};

export default useMcpServerListItem;
