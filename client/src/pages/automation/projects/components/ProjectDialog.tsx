import Button from '@/components/Button/Button';
import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Category, Project, Tag} from '@/shared/middleware/automation/configuration';
import {useCreateProjectMutation, useUpdateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectCategoryKeys, useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {ProjectTagKeys, useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectDialogProps {
    onClose?: (project?: Project) => void;
    project?: Project;
    triggerNode?: ReactNode;
}

const ProjectDialog = ({onClose, project, triggerNode}: ProjectDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {captureProjectCreated} = useAnalytics();

    const form = useForm<Project>({
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
            workspaceId: project?.workspaceId,
        } as Project,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetProjectCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const onSuccess = (projectId: number | void) => {
        captureProjectCreated();

        if (!projectId && project) {
            projectId = project.id!;
        }

        if (projectId) {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(projectId),
            });
        }

        queryClient.invalidateQueries({
            queryKey: ProjectCategoryKeys.projectCategories,
        });
        queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        queryClient.invalidateQueries({
            queryKey: ProjectTagKeys.projectTags,
        });

        closeDialog(project);
    };

    const createProjectMutation = useCreateProjectMutation({onSuccess});

    const updateProjectMutation = useUpdateProjectMutation({onSuccess});

    const tagNames = project?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog(project?: Project) {
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

        const tagValues = formData.tags?.map((tag: Tag) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name ? formData?.category : undefined;

        if (project?.id) {
            updateProjectMutation.mutate({
                ...project,
                ...formData,
                category,
            } as Project);
        } else {
            createProjectMutation.mutate({
                ...formData,
                category,
                tags: tagValues,
                workspaceId: currentWorkspaceId,
            } as Project);
        }
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveProject)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${project?.id ? 'Edit' : 'Create'} Project`}</DialogTitle>

                                <DialogDescription>
                                    {`Use this to ${
                                        project?.id ? 'edit' : 'create'
                                    } your project which will contain workflows`}
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        {categoriesError && !categoriesLoading && `An error has occurred: ${categoriesError.message}`}

                        {tagsError && !tagsLoading && `An error has occurred: ${tagsError.message}`}

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input placeholder="My CRM Project" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                        />

                        <FormField
                            control={control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="Cute description of your project" rows={5} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {!categoriesLoading && (
                            <FormField
                                control={control}
                                name="category"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Category</FormLabel>

                                        <FormControl>
                                            <CreatableSelect
                                                field={field}
                                                isMulti={false}
                                                onCreateOption={(inputValue: string) => {
                                                    setValue('category', {
                                                        label: inputValue,
                                                        name: inputValue,
                                                        value: inputValue,
                                                        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                                    } as any);
                                                }}
                                                options={categories!.map((category: Category) => ({
                                                    label: category.name,
                                                    value: category.name.toLowerCase().replace(/\W/g, ''),
                                                    ...category,
                                                }))}
                                                placeholder="Marketing, Sales, Social Media..."
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        {remainingTags && (
                            <FormField
                                control={control}
                                name="tags"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Tags</FormLabel>

                                        <FormControl>
                                            <CreatableSelect
                                                field={field}
                                                isMulti
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
                                                options={remainingTags!.map((tag: Tag) => {
                                                    return {
                                                        label: tag.name,
                                                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                        ...tag,
                                                    };
                                                })}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDialog;
