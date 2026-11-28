import useMcpServerListItemClick from '@/shared/components/mcp-server/hooks/useMcpServerListItemClick';
import {
    McpServer,
    useDeleteWorkspaceMcpServerMutation,
    useEnvironmentsQuery,
    useUpdateMcpServerMutation,
    useUpdateMcpServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpServerListItem = (mcpServer: McpServer) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showPromotionDialog, setShowPromotionDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const {handleMcpServerListItemClick, toolsCollapsibleTriggerRef} = useMcpServerListItemClick();

    const environmentsQuery = useEnvironmentsQuery();

    const showPromoteToEnvironment = (environmentsQuery.data?.environments?.length ?? 0) >= 2;

    const updateMcpServerMutation = useUpdateMcpServerMutation();
    const deleteWorkspaceMcpServerMutation = useDeleteWorkspaceMcpServerMutation();
    const updateMcpServerTagsMutation = useUpdateMcpServerTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
            void queryClient.invalidateQueries({queryKey: ['workspaceMcpServerTags']});
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
                    void queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
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
                    void queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
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
        setShowPromotionDialog,
        showDeleteDialog,
        showEditDialog,
        showPromoteToEnvironment,
        showPromotionDialog,
        toolsCollapsibleTriggerRef,
        updateMcpServerTagsMutation,
    };
};

export default useMcpServerListItem;
