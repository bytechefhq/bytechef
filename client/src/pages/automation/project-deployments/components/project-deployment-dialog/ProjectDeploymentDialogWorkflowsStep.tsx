import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';

export interface ProjectDeploymentDialogWorkflowsStepProps {
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    groupConnections?: boolean;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflows: Workflow[];
}

const ProjectDeploymentDialogWorkflowsStep = ({
    control,
    formState,
    groupConnections,
    setValue,
    workflows,
}: ProjectDeploymentDialogWorkflowsStepProps) => (
    <div className="h-full space-y-4">
        {workflows?.map((workflow, workflowIndex) => (
            <ProjectDeploymentDialogWorkflowsStepItem
                control={control}
                formState={formState}
                groupConnections={groupConnections}
                key={workflow.id!}
                label={workflow.label!}
                setValue={setValue}
                workflow={workflow}
                workflowIndex={workflowIndex}
            />
        ))}
    </div>
);

export default ProjectDeploymentDialogWorkflowsStep;
