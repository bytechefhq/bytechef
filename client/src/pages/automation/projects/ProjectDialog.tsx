import React, {useState} from 'react';

import Input from 'components/Input/Input';
import Dialog from 'components/Dialog/Dialog';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import TextArea from 'components/TextArea/TextArea';
import {Controller, useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {
    ProjectKeys,
    useGetProjectCategoriesQuery,
    useGetProjectTagsQuery,
} from '../../../queries/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CategoryModel, TagModel} from '../../../middleware/project';
import {
    useCreateProjectMutation,
    useUpdateProjectMutation,
} from '../../../mutations/projects.mutations';
import {Close} from '@radix-ui/react-dialog';
import {ProjectModel} from '../../../middleware/project';

interface ProjectDialogProps {
    project: ProjectModel | undefined;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

const ProjectDialog = ({
    project,
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
    } = useForm<ProjectModel>({
        defaultValues: {
            category: project?.category
                ? {
                      label: project?.category?.name,
                      ...project?.category,
                  }
                : undefined,
            description: project?.description || '',
            name: project?.name || '',
            tags:
                project?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as ProjectModel,
    });

    const {
        isLoading: categoriesIsLoading,
        error: categoriesError,
        data: categories,
    } = useGetProjectCategoriesQuery();

    const {
        isLoading: tagsIsLoading,
        error: tagsError,
        data: tags,
    } = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const tagNames = project?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    const createMutation = useCreateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectCategories);
            queryClient.invalidateQueries(ProjectKeys.projects);
            queryClient.invalidateQueries(ProjectKeys.projectTags);

            closeDialog();
        },
    });

    const updateMutation = useUpdateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectCategories);

            queryClient.invalidateQueries(ProjectKeys.projects);

            queryClient.invalidateQueries(ProjectKeys.projectTags);

            closeDialog();
        },
    });

    function closeDialog() {
        reset();

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function createProject() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name
            ? formData?.category
            : undefined;

        if (project?.id) {
            updateMutation.mutate({
                ...project,
                ...formData,
                category,
            } as ProjectModel);
        } else {
            createMutation.mutate({
                ...formData,
                category,
                tags: tagValues,
            } as ProjectModel);
        }
    }

    return (
        <Dialog
            description={`Use this to ${
                project?.id ? 'edit' : 'create'
            } your project which will contain related workflows`}
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title={`${project?.id ? 'Edit' : 'Create'} Project`}
            triggerLabel={
                showTrigger
                    ? `${project?.id ? 'Edit' : 'Create'} Project`
                    : undefined
            }
        >
            {categoriesError &&
                !categoriesIsLoading &&
                `An error has occurred: ${categoriesError.message}`}

            {tagsError &&
                !tagsIsLoading &&
                `An error has occurred: ${tagsError.message}`}

            <Input
                error={touchedFields.name && !!errors.name}
                label="Name"
                placeholder="My CRM Project"
                {...register('name', {required: true})}
            />

            <TextArea
                label="Description"
                placeholder="Cute description of your project"
                {...register('description')}
            />

            {!categoriesIsLoading && (
                <Controller
                    control={control}
                    name="category"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti={false}
                            label="Category"
                            options={categories!.map(
                                (category: CategoryModel) => ({
                                    label: `${category.name
                                        .charAt(0)
                                        .toUpperCase()}${category.name.slice(
                                        1
                                    )}`,
                                    value: category.name
                                        .toLowerCase()
                                        .replace(/\W/g, ''),
                                    ...category,
                                })
                            )}
                            placeholder="Marketing, Sales, Social Media..."
                            onCreateOption={(inputValue: string) => {
                                setValue('category', {
                                    label: inputValue,
                                    value: inputValue,
                                    name: inputValue,
                                    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                } as any);
                            }}
                        />
                    )}
                />
            )}

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

            <div className="mt-4 flex justify-end space-x-1">
                <Close asChild={true}>
                    <Button
                        displayType="lightBorder"
                        label="Cancel"
                        type="button"
                    />
                </Close>

                <Button
                    label="Save"
                    type="submit"
                    onClick={handleSubmit(createProject)}
                />
            </div>
        </Dialog>
    );
};

export default ProjectDialog;
