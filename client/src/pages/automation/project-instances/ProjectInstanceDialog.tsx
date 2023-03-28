import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import {useState} from 'react';
import {Controller, useForm} from 'react-hook-form';

import FilterableSelect from '../../../components/FilterableSelect/FilterableSelect';
import TextArea from '../../../components/TextArea/TextArea';
import {
    ProjectInstanceModel,
    ProjectModel,
    TagModel,
} from '../../../middleware/project';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from '../../../mutations/projects.mutations';
import {
    ProjectKeys,
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '../../../queries/projects.queries';

interface ProjectDialogProps {
    projectInstance: ProjectInstanceModel | undefined;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

const ProjectInstanceDialog = ({
    projectInstance,
    showTrigger = true,
    visible = false,
    onClose,
}: ProjectDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {
        control,
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        reset,
        setValue,
    } = useForm<ProjectInstanceModel>({
        defaultValues: {
            description: projectInstance?.description || '',
            project: projectInstance?.project
                ? {
                      label: projectInstance?.project?.name,
                      ...projectInstance?.project,
                  }
                : undefined,
            name: projectInstance?.name || '',
            tags:
                projectInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as ProjectInstanceModel,
    });

    const {
        isLoading: projectsLoading,
        error: projectsError,
        data: projects,
    } = useGetProjectsQuery({});

    const {
        isLoading: tagsLoading,
        error: tagsError,
        data: tags,
    } = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const createProjectInstanceMutation = useCreateProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
            queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
            queryClient.invalidateQueries(ProjectKeys.projectList({}));

            closeDialog();
        },
    });

    const updateProjectInstanceMutation = useUpdateProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
            queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
            queryClient.invalidateQueries(ProjectKeys.projectList({}));

            closeDialog();
        },
    });

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog() {
        reset();

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveProjectInstance() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const project = formData?.project?.name ? formData?.project : undefined;

        if (projectInstance?.id) {
            updateProjectInstanceMutation.mutate({
                ...projectInstance,
                ...formData,
                projectId: project?.id,
            } as ProjectInstanceModel);
        } else {
            createProjectInstanceMutation.mutate({
                ...formData,
                projectId: project?.id,
                tags: tagValues,
            } as ProjectInstanceModel);
        }
    }

    return (
        <Dialog
            description={`Use this to ${
                projectInstance?.id ? 'edit' : 'create'
            } your project instance with own parameters and connections`}
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title={`${projectInstance?.id ? 'Edit' : 'Create'} Instance`}
            triggerLabel={
                showTrigger
                    ? `${projectInstance?.id ? 'Edit' : 'Create'} Instance`
                    : undefined
            }
        >
            {projectsError &&
                !projectsLoading &&
                `An error has occurred: ${projectsError.message}`}

            {tagsError &&
                !tagsLoading &&
                `An error has occurred: ${tagsError.message}`}

            {!projectsLoading && (
                <Controller
                    control={control}
                    name="project"
                    render={({field}) => (
                        <FilterableSelect
                            field={field}
                            isMulti={false}
                            label="Project"
                            options={projects!.map((project: ProjectModel) => ({
                                label: `${project.name
                                    .charAt(0)
                                    .toUpperCase()}${project.name.slice(1)}`,
                                value: project.name
                                    .toLowerCase()
                                    .replace(/\W/g, ''),
                                ...project,
                            }))}
                            placeholder="Select..."
                            required
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
                            options={remainingTags!.map((tag: TagModel) => {
                                return {
                                    label: `${tag.name
                                        .charAt(0)
                                        .toUpperCase()}${tag.name.slice(1)}`,
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
                                        value: inputValue,
                                        name: inputValue,
                                    },
                                ] as never[]);
                            }}
                        />
                    )}
                />
            )}

            <div className="mt-8 flex justify-end space-x-1">
                <Close asChild>
                    <Button
                        displayType="lightBorder"
                        label="Cancel"
                        type="button"
                    />
                </Close>

                <Button
                    label="Save"
                    type="submit"
                    onClick={handleSubmit(saveProjectInstance)}
                />
            </div>
        </Dialog>
    );
};

export default ProjectInstanceDialog;
