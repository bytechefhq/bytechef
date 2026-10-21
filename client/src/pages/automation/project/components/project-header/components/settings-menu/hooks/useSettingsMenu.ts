import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/shared/mutations/automation/projectGit.mutations';
import {
    ProjectGitConfigurationKeys,
    useGetProjectGitConfigurationQuery,
} from '@/ee/shared/mutations/automation/projectGit.queries';
import {clearWorkflowHistory} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Project, ProjectApi, Workflow} from '@/shared/middleware/automation/configuration';
import {
    useDeleteProjectMutation,
    useDeleteProjectVersionMutation,
    useDuplicateProjectMutation,
    useRestoreProjectVersionMutation,
} from '@/shared/mutations/automation/projects.mutations';
import {
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectCategoryKeys} from '@/shared/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/shared/queries/automation/projectTags.queries';
import {ProjectVersionKeys, useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

export const useSettingsMenu = ({project, workflow}: {project: Project; workflow: Workflow}) => {
    const {data: projectGitConfiguration} = useGetProjectGitConfigurationQuery(project.id!);

    const projectId = project.id;

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {data: projectVersions} = useGetProjectVersionsQuery(project.id!);

    const queryClient = useQueryClient();

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: (_, projectId) => {
            navigate('/automation/projects');

            queryClient.cancelQueries({queryKey: ProjectKeys.project(projectId)});
            queryClient.removeQueries({queryKey: ProjectKeys.project(projectId)});

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories(project.workspaceId!),
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`);

            toast('Project duplicated successfully.');
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Workflow duplicated successfully.');
        },
    });

    const pullProjectFromGitMutation = usePullProjectFromGitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(project.id!)});

            queryClient.invalidateQueries({
                queryKey: ProjectVersionKeys.projectProjectVersions(project.id!),
            });

            toast('Project pulled from git repository successfully.');
        },
    });

    const updateProjectGitConfigurationMutation = useUpdateProjectGitConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(project.id!),
            });
        },
    });

    const deleteProjectVersionMutation = useDeleteProjectVersionMutation({
        onSuccess: (_, {projectVersion}) => {
            queryClient.invalidateQueries({
                queryKey: ProjectVersionKeys.projectProjectVersions(project.id!),
            });

            toast(`Version V${projectVersion} deleted.`);
        },
    });

    const restoreProjectVersionMutation = useRestoreProjectVersionMutation({
        onSuccess: async (_, {id, projectVersion}) => {
            const restoredProject = await queryClient.fetchQuery({
                queryFn: () => new ProjectApi().getProject({id}),
                queryKey: ProjectKeys.project(id),
                staleTime: 0,
            });

            const currentProjectWorkflowId = workflow.projectWorkflowId;
            const restoredProjectWorkflowIds = restoredProject.projectWorkflowIds ?? [];

            if (currentProjectWorkflowId && !restoredProjectWorkflowIds.includes(currentProjectWorkflowId)) {
                const firstProjectWorkflowId = restoredProjectWorkflowIds[0];

                navigate(
                    firstProjectWorkflowId
                        ? `/automation/projects/${id}/project-workflows/${firstProjectWorkflowId}?${searchParams}`
                        : '/automation/projects'
                );

                queryClient.removeQueries({
                    queryKey: ProjectWorkflowKeys.projectWorkflow(id, currentProjectWorkflowId),
                });
            }

            clearWorkflowHistory();

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(id)});
            queryClient.invalidateQueries({queryKey: ProjectVersionKeys.projectProjectVersions(id)});

            toast(`Version V${projectVersion} applied to the draft.`);
        },
    });

    const handleDeleteProjectAlertDialogClick = () => {
        if (project.id) {
            deleteProjectMutation.mutate(project.id);

            navigate('/automation/projects');
        }
    };

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            const deletedWorkflowId = (workflow as Workflow).projectWorkflowId;

            const firstRemainingWorkflowId = project?.projectWorkflowIds?.find(
                (projectWorkflowId) => projectWorkflowId !== deletedWorkflowId
            );

            if (!projectId || !deletedWorkflowId) {
                return;
            }

            queryClient.removeQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, deletedWorkflowId),
            });

            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
            });

            queryClient.invalidateQueries({
                exact: true,
                queryKey: ProjectKeys.project(projectId),
            });

            const baseUrl = '/automation/projects';
            let firstRemainingWorkflowUrlSuffix = '';

            if (firstRemainingWorkflowId) {
                firstRemainingWorkflowUrlSuffix = `/${projectId}/project-workflows/${firstRemainingWorkflowId}?${searchParams}`;
            }

            navigate(baseUrl + firstRemainingWorkflowUrlSuffix);
        },
    });

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (project.id && workflow.id) {
            deleteWorkflowMutation.mutate({
                id: workflow.id!,
            });
        }
    };

    const handleDeleteProjectVersion = (projectVersion: number, onSuccess: () => void) => {
        deleteProjectVersionMutation.mutate({id: project.id!, projectVersion}, {onSuccess});
    };

    const handleDuplicateProjectClick = () => {
        duplicateProjectMutation.mutate(project.id!);
    };

    const handleDuplicateWorkflowClick = () => {
        duplicateWorkflowMutation.mutate({
            id: project.id!,
            workflowId: workflow.id!,
        });
    };

    const handlePullProjectFromGitClick = () => {
        pullProjectFromGitMutation.mutate({id: project.id!});
    };

    const handleRestoreProjectVersion = (projectVersion: number, onSuccess: () => void) => {
        restoreProjectVersionMutation.mutate({id: project.id!, projectVersion}, {onSuccess});
    };

    const handleUpdateProjectGitConfigurationSubmit = ({
        onSuccess,
        projectGitConfiguration,
    }: {
        projectGitConfiguration: {branch: string; enabled: boolean};
        onSuccess: () => void;
    }) => {
        updateProjectGitConfigurationMutation.mutate(
            {
                id: project.id!,
                projectGitConfiguration,
            },
            {
                onSuccess,
            }
        );
    };

    return {
        deleteProjectVersionPending: deleteProjectVersionMutation.isPending,
        handleDeleteProjectAlertDialogClick,
        handleDeleteProjectVersion,
        handleDeleteWorkflowAlertDialogClick,
        handleDuplicateProjectClick,
        handleDuplicateWorkflowClick,
        handlePullProjectFromGitClick,
        handleRestoreProjectVersion,
        handleUpdateProjectGitConfigurationSubmit,
        projectGitConfiguration,
        projectVersions,
        restoreProjectVersionPending: restoreProjectVersionMutation.isPending,
    };
};
