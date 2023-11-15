import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {useGetProjectInstanceTagsQuery} from '@/queries/projectInstanceTags.queries';
import {useGetProjectsQuery} from '@/queries/projects.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Input from 'components/Input/Input';
import {
    ProjectInstanceModel,
    ProjectModel,
} from 'middleware/helios/configuration';
import {
    Control,
    Controller,
    UseFormGetValues,
    UseFormRegister,
    UseFormReturn,
    UseFormSetValue,
} from 'react-hook-form';

import TextArea from '../../../components/TextArea/TextArea';

interface ProjectDialogBasicStepProps {
    control: Control<ProjectInstanceModel>;
    errors: UseFormReturn<ProjectInstanceModel>['formState']['errors'];
    getValues: UseFormGetValues<ProjectInstanceModel>;
    projectInstance: ProjectInstanceModel | undefined;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
}

const ProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">
            {project?.tags?.map((tag) => tag.name).join(', ')}
        </span>
    </div>
);

const ProjectInstanceDialogBasicStep = ({
    control,
    getValues,
    projectInstance,
    register,
    setValue,
    touchedFields,
}: ProjectDialogBasicStepProps) => {
    const {
        data: projects,
        error: projectsError,
        isLoading: projectsLoading,
    } = useGetProjectsQuery({published: true});

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsLoading,
    } = useGetProjectInstanceTagsQuery();

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
                    {!projectInstance?.id && (
                        <Controller
                            control={control}
                            name="projectId"
                            render={({field}) => (
                                <ComboBox
                                    field={field}
                                    items={projects.map(
                                        (project) =>
                                            ({
                                                label: (
                                                    <ProjectLabel
                                                        project={project}
                                                    />
                                                ),
                                                value: project.id,
                                            }) as ComboBoxItemType
                                    )}
                                    label="Project"
                                    name="project"
                                    onChange={(item) => {
                                        if (item) {
                                            setValue('projectId', item.value);
                                        }
                                    }}
                                />
                            )}
                            rules={{required: true}}
                            shouldUnregister={false}
                        />
                    )}

                    <Input
                        error={touchedFields.name && !getValues('name')}
                        label="Name"
                        placeholder="My CRM Project - Production"
                        required
                        {...register('name', {
                            required: true,
                        })}
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
                                    options={remainingTags.map((tag) => {
                                        return {
                                            label: tag.name,
                                            value: tag.name
                                                .toLowerCase()
                                                .replace(/\W/g, ''),
                                            ...tag,
                                        };
                                    })}
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

export default ProjectInstanceDialogBasicStep;
