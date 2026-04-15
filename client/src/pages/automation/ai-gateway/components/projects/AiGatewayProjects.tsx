import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiGatewayProjectsQuery, useDeleteAiGatewayProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FolderIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiGatewayProjectType} from '../../types';
import AiGatewayProjectDialog from './AiGatewayProjectDialog';

const AiGatewayProjects = () => {
    const [deletingProjectId, setDeletingProjectId] = useState<string | undefined>(undefined);
    const [editingProject, setEditingProject] = useState<AiGatewayProjectType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: projectsData, isLoading: projectsIsLoading} = useAiGatewayProjectsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteProjectMutation = useDeleteAiGatewayProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayProjects']});

            setDeletingProjectId(undefined);
        },
    });

    const projects = projectsData?.aiGatewayProjects ?? [];

    const handleConfirmDelete = useCallback(() => {
        if (deletingProjectId) {
            deleteProjectMutation.mutate({id: deletingProjectId});
        }
    }, [deleteProjectMutation, deletingProjectId]);

    const handleEditProject = useCallback((project: AiGatewayProjectType) => {
        setEditingProject(project);
        setShowDialog(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingProject(undefined);
    }, []);

    if (projectsIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {projects.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Project" onClick={() => setShowDialog(true)} />}
                    icon={<FolderIcon className="size-12 text-muted-foreground" />}
                    message="Configure projects to organize your LLM Gateway usage."
                    title="No Projects Configured"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Project"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Slug</th>

                                    <th className="pb-2 font-medium">Description</th>

                                    <th className="pb-2 font-medium">Routing Policy</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {projects.map((project) => (
                                    <tr className="border-b" key={project.id}>
                                        <td className="py-3 font-medium">{project.name}</td>

                                        <td className="py-3">
                                            <span
                                                className={twMerge(
                                                    'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-800'
                                                )}
                                            >
                                                {project.slug}
                                            </span>
                                        </td>

                                        <td className="py-3 text-muted-foreground">
                                            {project.description || 'No description'}
                                        </td>

                                        <td className="py-3 text-muted-foreground">
                                            {project.routingPolicyId || 'Default'}
                                        </td>

                                        <td className="py-3">
                                            <div className="flex gap-2">
                                                <button
                                                    className="text-muted-foreground hover:text-foreground"
                                                    onClick={() => handleEditProject(project)}
                                                >
                                                    <PencilIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="text-destructive hover:text-destructive/80"
                                                    onClick={() => setDeletingProjectId(project.id)}
                                                >
                                                    <TrashIcon className="size-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <AlertDialog open={!!deletingProjectId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the project and all its
                            associated configuration.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingProjectId(undefined)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={handleConfirmDelete}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showDialog && currentWorkspaceId != null && (
                <AiGatewayProjectDialog
                    onClose={handleCloseDialog}
                    project={editingProject}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiGatewayProjects;
