import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';

export interface ProjectDeploymentDialogWorkflowsStepProps {
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    connectionsGrouped?: boolean;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflows: Workflow[];
}

const ProjectDeploymentDialogWorkflowsStep = ({
    connectionsGrouped,
    control,
    formState,
    setValue,
    workflows,
}: ProjectDeploymentDialogWorkflowsStepProps) => (
    <div className="h-full space-y-4">
        {workflows?.map((workflow, workflowIndex) => (
            <ProjectDeploymentDialogWorkflowsStepItem
                connectionsGrouped={connectionsGrouped}
                control={control}
                formState={formState}
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
