import {
    CategoryModel,
    ProjectModel,
    TagModel,
} from '@/middleware/helios/configuration';
import {
    useCreateProjectMutation,
    useUpdateProjectMutation,
} from '@/mutations/projects.mutations';
import {
    ProjectCategoryKeys,
    useGetProjectCategoriesQuery,
} from '@/queries/projectCategories.queries';
import {
    ProjectTagKeys,
    useGetProjectTagsQuery,
} from '@/queries/projectTags.quries';
import {ProjectKeys} from '@/queries/projects.queries';
import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {useState} from 'react';
import {Controller, useForm} from 'react-hook-form';

interface ProjectDialogProps {
    project?: ProjectModel;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: (project?: ProjectModel) => void;
}

const ProjectDialog = ({
    onClose,
    project,
    showTrigger = true,
    visible = false,
}: ProjectDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {
        control,
        formState: {errors, touchedFields},
        getValues,
        handleSubmit,
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
        data: categories,
        error: categoriesError,
        isLoading: categoriesLoading,
    } = useGetProjectCategoriesQuery();

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsLoading,
    } = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const createProjectMutation = useCreateProjectMutation({
        onSuccess: (project: ProjectModel) => {
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });

            closeDialog(project);
        },
    });

    const updateProjectMutation = useUpdateProjectMutation({
        onSuccess: (project: ProjectModel) => {
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });

            closeDialog(project);
        },
    });

    const tagNames = project?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog(project?: ProjectModel) {
        reset();

        setIsOpen(false);

        if (onClose) {
            onClose(project);
        }
    }

    function saveProject() {
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
            updateProjectMutation.mutate({
                ...project,
                ...formData,
                category,
            } as ProjectModel);
        } else {
            createProjectMutation.mutate({
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
                    closeDialog(project);
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
                !categoriesLoading &&
                `An error has occurred: ${categoriesError.message}`}

            {tagsError &&
                !tagsLoading &&
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

            {!categoriesLoading && (
                <Controller
                    control={control}
                    name="category"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti={false}
                            label="Category"
                            onCreateOption={(inputValue: string) => {
                                setValue('category', {
                                    label: inputValue,
                                    name: inputValue,
                                    value: inputValue,
                                    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                } as any);
                            }}
                            options={categories!.map(
                                (category: CategoryModel) => ({
                                    label: category.name,
                                    value: category.name
                                        .toLowerCase()
                                        .replace(/\W/g, ''),
                                    ...category,
                                })
                            )}
                            placeholder="Marketing, Sales, Social Media..."
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
                            options={remainingTags!.map((tag: TagModel) => {
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
                    onClick={handleSubmit(saveProject)}
                    type="submit"
                />
            </div>
        </Dialog>
    );
};

export default ProjectDialog;
