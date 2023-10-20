import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Input from 'components/Input/Input';
import {
    ProjectInstanceModel,
    ProjectModel,
    TagModel,
} from 'middleware/automation/project';
import {
    Control,
    Controller,
    FieldErrors,
    UseFormGetValues,
    UseFormRegister,
    UseFormReturn,
    UseFormSetValue,
} from 'react-hook-form';

import FilterableSelect from '../../../components/FilterableSelect/FilterableSelect';
import TextArea from '../../../components/TextArea/TextArea';
import {
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '../../../queries/projects.queries';

interface ProjectDialogProps {
    projectInstance: ProjectInstanceModel | undefined;
    control: Control<ProjectInstanceModel>;
    errors: FieldErrors<ProjectInstanceModel>;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
}

const StepBasic = ({
    projectInstance,
    control,
    register,
    setValue,
    getValues,
    errors,
    touchedFields,
}: ProjectDialogProps) => {
    const {
        isLoading: projectsLoading,
        error: projectsError,
        data: projects,
    } = useGetProjectsQuery();

    const {
        isLoading: tagsLoading,
        error: tagsError,
        data: tags,
    } = useGetProjectTagsQuery();

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return (
        <div>
            {projectsError && !projectsLoading && (
                <span>An error has occurred: {projectsError.message}</span>
            )}

            {tagsError && !tagsLoading && (
                <span>An error has occurred: {tagsError.message}</span>
            )}

            {!projectsLoading && (
                <Controller
                    control={control}
                    name="project"
                    render={({field}) => (
                        <FilterableSelect
                            field={field}
                            label="Project"
                            options={projects!.map((project: ProjectModel) => ({
                                label: project.name,
                                value: project.id!.toString(),
                            }))}
                            placeholder="Select..."
                            required
                            onChange={(selectedOption) => {
                                setValue(
                                    'projectId',
                                    Number.parseInt(selectedOption!.value!)
                                );
                            }}
                        />
                    )}
                />
            )}

            <Input
                error={touchedFields.name && !!errors.name}
                label="Name"
                placeholder="My CRM Project - Production"
                {...register('name', {required: true})}
            />

            <TextArea
                label="Description"
                placeholder="Cute description of your project instance"
                {...register('description')}
            />

            {remainingTags && (
                <Controller
                    control={control}
                    name="tags"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti
                            label="Tags"
                            options={remainingTags!.map((tag: TagModel) => ({
                                label: tag.name,
                                value: tag.name,
                            }))}
                            onCreateOption={(inputValue: string) => {
                                setValue('tags', [
                                    ...getValues().tags!,
                                    {
                                        label: inputValue,
                                        value: inputValue,
                                        name: inputValue,
                                    },
                                ] as never[]);
                            }}
                        />
                    )}
                />
            )}
        </div>
    );
};

export default StepBasic;
