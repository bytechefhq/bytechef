import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
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
import {CategoryModel, ProjectModel, TagModel} from '@/middleware/automation/configuration';
import {useCreateProjectMutation, useUpdateProjectMutation} from '@/mutations/automation/projects.mutations';
import {ProjectCategoryKeys, useGetProjectCategoriesQuery} from '@/queries/automation/projectCategories.queries';
import {ProjectTagKeys, useGetProjectTagsQuery} from '@/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectDialogProps {
    onClose?: (project?: ProjectModel) => void;
    project?: ProjectModel;
    triggerNode?: ReactNode;
}

const ProjectDialog = ({onClose, project, triggerNode}: ProjectDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<ProjectModel>({
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

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetProjectCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const onSuccess = (project: ProjectModel) => {
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

        const category = formData?.category?.name ? formData?.category : undefined;

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
                    <DialogHeader>
                        <div className="flex items-center justify-between">
                            <DialogTitle>{`${project?.id ? 'Edit' : 'Create'} Project`}</DialogTitle>

                            <DialogClose asChild>
                                <Button size="icon" variant="ghost">
                                    <Cross2Icon className="size-4 opacity-70" />
                                </Button>
                            </DialogClose>
                        </div>

                        <DialogDescription>
                            {`Use this to ${
                                project?.id ? 'edit' : 'create'
                            } your project which will contain related workflows`}
                        </DialogDescription>
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
                                    <Textarea placeholder="Cute description of your project" {...field} />
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
                                            options={categories!.map((category: CategoryModel) => ({
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
                                            options={remainingTags!.map((tag: TagModel) => {
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
                            <Button type="button" variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button onClick={handleSubmit(saveProject)} type="submit">
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDialog;
