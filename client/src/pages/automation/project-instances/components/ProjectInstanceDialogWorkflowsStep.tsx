import ProjectInstanceDialogWorkflowsStepItem from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItem';
import {ProjectInstanceModel, WorkflowModel} from '@/shared/middleware/automation/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface ProjectInstanceDialogWorkflowsStepProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    workflows: WorkflowModel[];
}

const ProjectInstanceDialogWorkflowsStep = ({
    control,
    formState,
    setValue,
    workflows,
}: ProjectInstanceDialogWorkflowsStepProps) => {
    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectInstanceDialogWorkflowsStepItem
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

export default ProjectInstanceDialogWorkflowsStep;
