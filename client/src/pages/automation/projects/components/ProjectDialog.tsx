import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Form} from '@/components/ui/form';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import CategoryFormField from '@/shared/components/entity-form/CategoryFormField';
import DescriptionFormField from '@/shared/components/entity-form/DescriptionFormField';
import NameFormField from '@/shared/components/entity-form/NameFormField';
import TagsFormField from '@/shared/components/entity-form/TagsFormField';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {useCreateProjectMutation, useUpdateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectCategoryKeys, useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {ProjectTagKeys, useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectDialogProps {
    onClose?: (project?: Project) => void;
    onSuccess?: (projectId: number | void) => void;
    project?: Project;
    triggerNode?: ReactNode;
}

const ProjectDialog = ({onClose, onSuccess, project, triggerNode}: ProjectDialogProps) => {
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

    const {getValues, handleSubmit, reset} = form;

    const {
        data: categories,
        error: categoriesError,
        isLoading: categoriesLoading,
    } = useGetProjectCategoriesQuery(currentWorkspaceId!);

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const onSuccessHandler = (projectId: number | void) => {
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
            queryKey: ProjectCategoryKeys.projectCategories(currentWorkspaceId!),
        });
        queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        queryClient.invalidateQueries({
            queryKey: ProjectTagKeys.projectTags,
        });

        if (onSuccess) {
            onSuccess(projectId);
        }

        closeDialog(project);
    };

    const createProjectMutation = useCreateProjectMutation({onSuccess: onSuccessHandler});

    const updateProjectMutation = useUpdateProjectMutation({onSuccess: onSuccessHandler});

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

            <DialogContent aria-label="Project Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveProject)}>
                            <DialogHeader
                                description={`Use this to ${
                                    project?.id ? 'edit' : 'create'
                                } your project which will contain workflows`}
                                title={`${project?.id ? 'Edit' : 'Create'} Project`}
                            />

                            <DialogBody>
                                {categoriesError &&
                                    !categoriesLoading &&
                                    `An error has occurred: ${categoriesError.message}`}

                                {tagsError && !tagsLoading && `An error has occurred: ${tagsError.message}`}

                                <NameFormField placeholder="My CRM Project" />

                                <DescriptionFormField placeholder="Cute description of your project" />

                                <CategoryFormField categories={categories} categoriesLoading={categoriesLoading} />

                                <TagsFormField remainingTags={remainingTags} />
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDialog;
