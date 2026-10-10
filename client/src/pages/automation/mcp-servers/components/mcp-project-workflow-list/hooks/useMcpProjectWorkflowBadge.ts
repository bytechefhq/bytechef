import {useDeleteMcpProjectWorkflowMutation, useUpdateMcpProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

import {McpProjectWorkflowItemType} from './useMcpProjectList';

export default function useMcpProjectWorkflowBadge(mcpProjectWorkflow: McpProjectWorkflowItemType) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const queryClient = useQueryClient();

    const projectDeploymentId = mcpProjectWorkflow.projectDeploymentWorkflow?.projectDeploymentId;

    const {data: projectDeployment} = useGetProjectDeploymentQuery(
        projectDeploymentId ? +projectDeploymentId : 0,
        !!projectDeploymentId
    );

    const projectDeploymentWorkflow = useMemo(
        () =>
            projectDeployment?.projectDeploymentWorkflows?.find(
                (deploymentWorkflow) => deploymentWorkflow.id === +mcpProjectWorkflow.projectDeploymentWorkflowId
            ),
        [projectDeployment?.projectDeploymentWorkflows, mcpProjectWorkflow.projectDeploymentWorkflowId]
    );

    const {data: workflow} = useGetWorkflowQuery(
        projectDeploymentWorkflow?.workflowId ?? '',
        !!projectDeploymentWorkflow?.workflowId
    );

    const deleteMcpProjectWorkflowMutation = useDeleteMcpProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });

            setShowDeleteDialog(false);
        },
    });

    const updateMcpProjectWorkflowMutation = useUpdateMcpProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });
        },
    });

    const handleCloseEditDialog = () => {
        setShowEditWorkflowDialog(false);

        queryClient.invalidateQueries({
            queryKey: ['mcpProjectsByServerId'],
        });
    };

    const handleEnabledChange = (enabled: boolean) => {
        updateMcpProjectWorkflowMutation.mutate({
            id: mcpProjectWorkflow.id,
            input: {enabled},
        });
    };

    const handleConfirmDelete = () => {
        deleteMcpProjectWorkflowMutation.mutate({
            id: mcpProjectWorkflow.id,
        });
    };

    return {
        handleCloseEditDialog,
        handleConfirmDelete,
        handleEnabledChange,
        isDeletePending: deleteMcpProjectWorkflowMutation.isPending,
        isEnablePending: updateMcpProjectWorkflowMutation.isPending,
        projectDeploymentWorkflow,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    };
}
