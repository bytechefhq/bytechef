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
import {useState} from 'react';

interface ProjectDialogProps {
    projectInstance: ProjectInstanceModel | undefined;
    control: Control<ProjectInstanceModel>;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
    errors: UseFormReturn<ProjectInstanceModel>['formState']['errors'];
}

const StepBasic = ({
    control,
    touchedFields,
    getValues,
    projectInstance,
    register,
    setValue,
}: ProjectDialogProps) => {
    const {
        data: projects,
        error: projectsError,
        isLoading: projectsLoading,
    } = useGetProjectsQuery();

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsLoading,
    } = useGetProjectTagsQuery();

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    const [name, setName] = useState<string | undefined>();

    const [projectId, setProjectId] = useState<number | undefined>();

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
                    rules={{required: true}}
                    render={({field}) => (
                        <FilterableSelect
                            error={
                                touchedFields.projectId &&
                                projectId === undefined
                            }
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
                                setProjectId(
                                    Number.parseInt(selectedOption!.value!)
                                );
                            }}
                        />
                    )}
                />
            )}

            <Input
                error={
                    touchedFields.name &&
                    (name === undefined || name.length === 0)
                }
                label="Name"
                placeholder="My CRM Project - Production"
                {...register('name', {required: true})}
                onChange={(e) => {
                    setName(e.target.value);
                }}
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
                                        name: inputValue,
                                        value: inputValue,
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

const Errors = ({errors}: {errors: string[]}) => (
    <ul>
        {errors.map((error, index) => (
            <li
                key={`error_${index}`}
                className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700"
            >
                An error has occurred: {error}
            </li>
        ))}
    </ul>
);

export default StepBasic;
