import {McpProject} from '@/shared/middleware/graphql';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

const useMcpProjectListItem = (mcpProject: McpProject) => {
    const [showEditWorkflowsDialog, setShowEditWorkflowsDialog] = useState(false);
    const [showChangeProjectVersionDialog, setShowChangeProjectVersionDialog] = useState(false);

    const {data: projectDeployment} = useGetProjectDeploymentQuery(+mcpProject.projectDeploymentId!);

    const queryClient = useQueryClient();

    const mcpDeploymentWorkflowIds = useMemo(
        () =>
            new Set(
                mcpProject.mcpProjectWorkflows
                    ?.filter((workflow) => workflow != null)
                    .map((workflow) => String(workflow.projectDeploymentWorkflowId)) || []
            ),
        [mcpProject.mcpProjectWorkflows]
    );

    const mcpWorkflowUuids = useMemo(
        () =>
            projectDeployment?.projectDeploymentWorkflows
                ?.filter(
                    (deploymentWorkflow) =>
                        deploymentWorkflow.workflowUuid != null &&
                        mcpDeploymentWorkflowIds.has(String(deploymentWorkflow.id))
                )
                .map((deploymentWorkflow) => deploymentWorkflow.workflowUuid!) || [],
        [mcpDeploymentWorkflowIds, projectDeployment?.projectDeploymentWorkflows]
    );

    const handleOnProjectDeploymentDialogClose = () => {
        queryClient
            .invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            })
            .then(() => setShowChangeProjectVersionDialog(false));
    };

    return {
        handleOnProjectDeploymentDialogClose,
        mcpWorkflowUuids,
        projectDeployment,
        setShowChangeProjectVersionDialog,
        setShowEditWorkflowsDialog,
        showChangeProjectVersionDialog,
        showEditWorkflowsDialog,
    };
};

export default useMcpProjectListItem;
