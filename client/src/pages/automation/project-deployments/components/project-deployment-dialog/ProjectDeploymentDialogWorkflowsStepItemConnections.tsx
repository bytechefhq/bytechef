import ProjectDeploymentDialogWorkflowsStepItemConnection from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnection';
import {ComponentConnection, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {Control} from 'react-hook-form';

const ProjectDeploymentDialogWorkflowsStepItemConnections = ({
    componentConnections,
    control,
    workflowIndex,
}: {
    control: Control<ProjectDeployment>;
    componentConnections: ComponentConnection[];
    workflowIndex: number;
}) => (
    <>
        {!componentConnections.length && <p className="text-sm">No defined connections.</p>}

        <ul>
            {componentConnections.map((componentConnection, componentConnectionIndex) => (
                <ProjectDeploymentDialogWorkflowsStepItemConnection
                    componentConnection={componentConnection}
                    componentConnectionIndex={componentConnectionIndex}
                    control={control}
                    key={`${componentConnectionIndex}_${componentConnection.key}`}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    </>
);

export default ProjectDeploymentDialogWorkflowsStepItemConnections;
