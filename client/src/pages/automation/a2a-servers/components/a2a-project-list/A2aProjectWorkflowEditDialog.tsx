import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

import {A2aProjectItemType} from './hooks/useA2aProjectList';

interface A2aProjectWorkflowEditDialogProps {
    a2aProject: A2aProjectItemType;
    onClose: () => void;
    workflowId: string;
}

const A2aProjectWorkflowEditDialog = ({a2aProject, onClose, workflowId}: A2aProjectWorkflowEditDialogProps) => {
    const queryClient = useQueryClient();

    const projectDeploymentId = a2aProject.projectDeploymentId ? Number(a2aProject.projectDeploymentId) : 0;

    const {data: projectDeployment} = useGetProjectDeploymentQuery(projectDeploymentId, !!projectDeploymentId);

    const {data: workflow} = useGetWorkflowQuery(workflowId, !!workflowId);

    const projectDeploymentWorkflow = useMemo(
        () =>
            projectDeployment?.projectDeploymentWorkflows?.find(
                (deploymentWorkflow) => deploymentWorkflow.workflowId === workflowId
            ),
        [projectDeployment?.projectDeploymentWorkflows, workflowId]
    );

    const handleClose = () => {
        void queryClient.invalidateQueries({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});

        onClose();
    };

    if (!projectDeploymentWorkflow || !workflow) {
        return null;
    }

    return (
        <ProjectDeploymentEditWorkflowDialog
            onClose={handleClose}
            projectDeploymentWorkflow={projectDeploymentWorkflow}
            workflow={workflow}
        />
    );
};

export default A2aProjectWorkflowEditDialog;
