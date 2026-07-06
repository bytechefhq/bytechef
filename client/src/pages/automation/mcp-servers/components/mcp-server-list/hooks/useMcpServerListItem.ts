import {
    McpServer,
    useDeleteWorkspaceMcpServerMutation,
    useUpdateMcpServerMutation,
    useUpdateMcpServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useRef, useState} from 'react';

const useMcpServerListItem = (mcpServer: McpServer) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const toolsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

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

    const handleMcpServerListItemClick = useCallback((event: React.MouseEvent) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '.dropdown-menu-item',
            '[data-radix-dropdown-menu-item]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
            'button',
            'input',
            'svg',
        ].join(', ');

        if (target.closest(interactiveSelectors)) {
            return;
        }

        if (toolsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        toolsCollapsibleTriggerRef.current?.click();
    }, []);

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
