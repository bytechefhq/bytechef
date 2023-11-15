import ProjectInstanceDialogWorkflowListItem from '@/pages/automation/project-instances/ProjectInstanceDialogWorkflowListItem';
import {useGetProjectWorkflowsQuery} from '@/queries/projects.queries';
import {ProjectInstanceModel} from 'middleware/helios/configuration';
import {Control, UseFormGetValues, UseFormRegister} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface ProjectInstanceDialogWorkflowsStepProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    register: UseFormRegister<ProjectInstanceModel>;
}

const ProjectInstanceDialogWorkflowsStep = ({
    control,
    formState,
    getValues,
    register,
}: ProjectInstanceDialogWorkflowsStepProps) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(
        getValues().projectId!
    );

    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectInstanceDialogWorkflowListItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    register={register}
                    workflow={workflow}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default ProjectInstanceDialogWorkflowsStep;
