import ProjectInstanceDialogWorkflowsStepItemConnection from '@/pages/automation/project-instances/components/project-instance-dialog/ProjectInstanceDialogWorkflowsStepItemConnection';
import {ProjectInstance, WorkflowConnection} from '@/shared/middleware/automation/configuration';
import {Control} from 'react-hook-form';

const ProjectInstanceDialogWorkflowsStepItemConnections = ({
    control,
    workflowConnections,
    workflowIndex,
}: {
    control: Control<ProjectInstance>;
    workflowConnections: WorkflowConnection[];
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
