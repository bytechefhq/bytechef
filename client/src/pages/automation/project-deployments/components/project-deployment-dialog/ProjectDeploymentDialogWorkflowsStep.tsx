import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface ProjectDeploymentDialogWorkflowsStepProps {
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflows: Workflow[];
}

const ProjectDeploymentDialogWorkflowsStep = ({
    control,
    formState,
    setValue,
    workflows,
}: ProjectDeploymentDialogWorkflowsStepProps) => {
    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectDeploymentDialogWorkflowsStepItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    setValue={setValue}
                    workflow={workflow}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default ProjectDeploymentDialogWorkflowsStep;
