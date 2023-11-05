import ProjectInstanceDialogWorkflowListItem from '@/pages/automation/project-instances/ProjectInstanceDialogWorkflowListItem';
import {ProjectInstanceModel} from 'middleware/helios/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
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
        <ul className="space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectInstanceDialogWorkflowListItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    workflow={workflow}
                    label={workflow.label!}
                    register={register}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default ProjectInstanceDialogWorkflowsStep;
