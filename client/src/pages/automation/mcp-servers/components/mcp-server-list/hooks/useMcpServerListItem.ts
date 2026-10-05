import useMcpServerListItemClick from '@/shared/components/mcp-server/hooks/useMcpServerListItemClick';
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
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const {handleMcpServerListItemClick, toolsCollapsibleTriggerRef} = useMcpServerListItemClick();

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
                onSettled: () => {
                    setIsEnablePending(false);
                },
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                },
            }
        );
    };

    const handleDeleteClick = async () => {
        if (isPending) {
            return;
        }

        setIsPending(true);

        deleteWorkspaceMcpServerMutation.mutate(
            {
                id: mcpServer.id,
            },
            {
                onSettled: () => {
                    setIsPending(false);
                },
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
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
        isPending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
        toolsCollapsibleTriggerRef,
        updateMcpServerTagsMutation,
    };
};

export default useMcpServerListItem;
