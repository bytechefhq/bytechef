import ProjectDeploymentDialogWorkflowsStepItemConnection from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnection';
import {ProjectDeployment, WorkflowConnection} from '@/shared/middleware/automation/configuration';
import {Control} from 'react-hook-form';

const ProjectDeploymentDialogWorkflowsStepItemConnections = ({
    control,
    workflowConnections,
    workflowIndex,
}: {
    control: Control<ProjectDeployment>;
    workflowConnections: WorkflowConnection[];
    workflowIndex: number;
}) => {
    return workflowConnections.length ? (
        <>
            {workflowConnections.map((workflowConnection, workflowConnectionIndex) => (
                <ProjectDeploymentDialogWorkflowsStepItemConnection
                    control={control}
                    key={workflowConnectionIndex + '_' + workflowConnection.key}
                    workflowConnection={workflowConnection}
                    workflowConnectionIndex={workflowConnectionIndex}
                    workflowIndex={workflowIndex}
                />
            ))}
        </>
    ) : (
        <p className="text-sm">No defined connections.</p>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItemConnections;
