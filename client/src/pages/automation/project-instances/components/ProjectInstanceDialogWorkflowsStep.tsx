import ProjectInstanceDialogWorkflowsStepItem from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItem';
import {useGetProjectVersionWorkflowsQuery} from '@/queries/automation/workflows.queries';
import {ProjectInstanceModel} from 'middleware/automation/configuration';
import {Control, UseFormGetValues, UseFormRegister, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface ProjectInstanceDialogWorkflowsStepProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
}

const ProjectInstanceDialogWorkflowsStep = ({
    control,
    formState,
    getValues,
    register,
    setValue,
}: ProjectInstanceDialogWorkflowsStepProps) => {
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(getValues().projectId!, getValues().projectVersion!);

    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectInstanceDialogWorkflowsStepItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    register={register}
                    setValue={setValue}
                    workflowId={workflow.id!}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default ProjectInstanceDialogWorkflowsStep;
