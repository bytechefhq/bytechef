import ProjectInstanceDialogWorkflowsStepItemConnection from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItemConnection';
import {ProjectInstanceModel, WorkflowConnectionModel} from '@/shared/middleware/automation/configuration';
import {Control} from 'react-hook-form';

const ProjectInstanceDialogWorkflowsStepItemConnections = ({
    control,
    workflowConnections,
    workflowIndex,
}: {
    control: Control<ProjectInstanceModel>;
    workflowConnections: WorkflowConnectionModel[];
    workflowIndex: number;
}) => {
    return workflowConnections.length ? (
        <>
            {workflowConnections.map((workflowConnection, workflowConnectionIndex) => (
                <ProjectInstanceDialogWorkflowsStepItemConnection
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

export default ProjectInstanceDialogWorkflowsStepItemConnections;
