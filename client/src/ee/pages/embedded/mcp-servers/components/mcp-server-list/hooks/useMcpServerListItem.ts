import useMcpServerListItemClick from '@/shared/components/mcp-server/hooks/useMcpServerListItemClick';
import {
    McpServer,
    useDeleteEmbeddedMcpServerMutation,
    useUpdateEmbeddedMcpServerMutation,
    useUpdateEmbeddedMcpServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpServerListItem = (mcpServer: McpServer) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const {handleMcpServerListItemClick, toolsCollapsibleTriggerRef} = useMcpServerListItemClick();

    const updateEmbeddedMcpServerMutation = useUpdateEmbeddedMcpServerMutation();
    const deleteEmbeddedMcpServerMutation = useDeleteEmbeddedMcpServerMutation();
    const updateEmbeddedMcpServerTagsMutation = useUpdateEmbeddedMcpServerTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
            queryClient.invalidateQueries({queryKey: ['embeddedMcpServerTags']});
        },
    });

    const handleOnCheckedChange = async (value: boolean) => {
        setIsEnablePending(true);

        updateEmbeddedMcpServerMutation.mutate(
            {
                id: mcpServer.id,
                input: {
                    enabled: value,
                },
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
                    setIsEnablePending(false);
                },
            }
        );
    };

    const handleDeleteClick = async () => {
        deleteEmbeddedMcpServerMutation.mutate(
            {
                mcpServerId: mcpServer.id,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
                    setShowDeleteDialog(false);
                },
            }
        );
    };

    return {
        handleDeleteClick,
        handleMcpServerListItemClick,
        handleOnCheckedChange,
        isEnablePending,
        isPending: deleteEmbeddedMcpServerMutation.isPending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
        toolsCollapsibleTriggerRef,
        updateEmbeddedMcpServerTagsMutation,
    };
};

export default useMcpServerListItem;
