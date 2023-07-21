import {
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '@/queries/projects.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Input from 'components/Input/Input';
import {ProjectInstanceModel} from 'middleware/helios/configuration';
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

interface ProjectDialogProps {
    control: Control<ProjectInstanceModel>;
    errors: UseFormReturn<ProjectInstanceModel>['formState']['errors'];
    getValues: UseFormGetValues<ProjectInstanceModel>;
    projectInstance: ProjectInstanceModel | undefined;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
}

const InstanceDialogBasicStep = ({
    control,
    getValues,
    projectInstance,
    register,
    setValue,
    touchedFields,
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

    return (
        <div>
            {projectsError && !projectsLoading && (
                <span>An error has occurred: {projectsError.message}</span>
            )}

            {tagsError && !tagsLoading && (
                <span>An error has occurred: {tagsError.message}</span>
            )}

            {projects ? (
                <>
                    <Controller
                        control={control}
                        name="project"
                        rules={{required: true}}
                        render={({field}) => (
                            <FilterableSelect
                                autoFocus
                                error={
                                    touchedFields.projectId &&
                                    !getValues('projectId')
                                }
                                field={field}
                                label="Project"
                                onChange={(selectedOption) => {
                                    if (selectedOption) {
                                        setValue(
                                            'projectId',
                                            parseInt(
                                                selectedOption.value.toString()
                                            )
                                        );
                                        setValue('project', selectedOption);
                                    }
                                }}
                                options={projects.map((project) => ({
                                    label: project.name,
                                    name: project.name,
                                    value: project.id!.toString(),
                                }))}
                                placeholder="Select..."
                                required
                                value={
                                    (projectInstance?.project && {
                                        label: projectInstance.project.name,
                                        name: projectInstance.project.name,
                                        value: projectInstance.project.id!.toString(),
                                    }) ||
                                    undefined
                                }
                            />
                        )}
                        shouldUnregister={false}
                    />

                    <Input
                        error={touchedFields.name && !getValues('name')}
                        label="Name"
                        placeholder="My CRM Project - Production"
                        required
                        {...register('name', {
                            required: true,
                        })}
                        onChange={(e) => {
                            setValue('name', e.target.value);
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
                                    options={remainingTags.map((tag) => {
                                        return {
                                            label: `${tag.name
                                                .charAt(0)
                                                .toUpperCase()}${tag.name.slice(
                                                1
                                            )}`,
                                            value: tag.name
                                                .toLowerCase()
                                                .replace(/\W/g, ''),
                                            ...tag,
                                        };
                                    })}
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
                </>
            ) : (
                <span className="px-2">Loading...</span>
            )}
        </div>
    );
};

export default InstanceDialogBasicStep;
