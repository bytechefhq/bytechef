import useMcpServerListItemClick from '@/shared/components/mcp-server/hooks/useMcpServerListItemClick';
import {
    McpServer,
    useDeleteEmbeddedMcpServerMutation,
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
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const {handleMcpServerListItemClick, toolsCollapsibleTriggerRef} = useMcpServerListItemClick();

    const updateMcpServerMutation = useUpdateMcpServerMutation();
    const deleteEmbeddedMcpServerMutation = useDeleteEmbeddedMcpServerMutation();
    const updateMcpServerTagsMutation = useUpdateMcpServerTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
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
        setShowMcpComponentDialog,
        setShowWorkflowDialog,
        showDeleteDialog,
        showEditDialog,
        showMcpComponentDialog,
        showWorkflowDialog,
        toolsCollapsibleTriggerRef,
        updateMcpServerTagsMutation,
    };
};

export default useMcpServerListItem;
