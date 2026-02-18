import ProjectDeploymentDialogWorkflowsStepItemConnection from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnection';
import {ComponentConnection, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {Control} from 'react-hook-form';

interface ProjectDeploymentDialogWorkflowsStepItemConnectionsProps {
    componentConnections: ComponentConnection[];
    control: Control<ProjectDeployment>;
    workflowIndex: number;
    workflowNodeLabelMap: Map<string, string>;
}

const ProjectDeploymentDialogWorkflowsStepItemConnections = ({
    componentConnections,
    control,
    workflowIndex,
    workflowNodeLabelMap,
}: ProjectDeploymentDialogWorkflowsStepItemConnectionsProps) => (
    <>
        {!componentConnections.length && <p className="text-sm">No defined connections.</p>}

        <ul className="space-y-4">
            {componentConnections.map((componentConnection, componentConnectionIndex) => (
                <ProjectDeploymentDialogWorkflowsStepItemConnection
                    componentConnection={componentConnection}
                    componentConnectionIndex={componentConnectionIndex}
                    control={control}
                    key={`${componentConnectionIndex}_${componentConnection.key}`}
                    workflowIndex={workflowIndex}
                    workflowNodeLabel={workflowNodeLabelMap.get(componentConnection.workflowNodeName)}
                />
            ))}
        </ul>
    </>
);

export default ProjectDeploymentDialogWorkflowsStepItemConnections;
