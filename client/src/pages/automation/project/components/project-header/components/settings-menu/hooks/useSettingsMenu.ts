import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/shared/mutations/automation/projectGit.mutations';
import {
    ProjectGitConfigurationKeys,
    useGetProjectGitConfigurationQuery,
} from '@/ee/shared/mutations/automation/projectGit.queries';
import {useToast} from '@/hooks/use-toast';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useDeleteProjectMutation, useDuplicateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
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

export const useSettingsMenu = ({project, workflow}: {project: Project; workflow: Workflow}) => {
    const {data: projectGitConfiguration} = useGetProjectGitConfigurationQuery(project.id!);

    const projectId = project.id;

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const {toast} = useToast();

    const {data: projectVersions} = useGetProjectVersionsQuery(project.id!);

    const queryClient = useQueryClient();

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
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
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const pullProjectFromGitMutation = usePullProjectFromGitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(project.id!)});

            queryClient.invalidateQueries({
                queryKey: ProjectVersionKeys.projectProjectVersions(project.id!),
            });

            toast({description: 'Project pulled from git repository successfully.'});
        },
    });

    const updateProjectGitConfigurationMutation = useUpdateProjectGitConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectGitConfigurationKeys.projectGitConfiguration(project.id!),
            });
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

            // exact here prevents double fetchs of project
            queryClient.invalidateQueries({
                exact: true,
                queryKey: ProjectKeys.project(projectId),
            });

            const baseUrl = '/automation/projects';
            const firstRemainingWorkflowUrlSuffix = firstRemainingWorkflowId
                ? `/${projectId}/project-workflows/${firstRemainingWorkflowId}?${searchParams}`
                : '';
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
        handleDeleteProjectAlertDialogClick,
        handleDeleteWorkflowAlertDialogClick,
        handleDuplicateProjectClick,
        handleDuplicateWorkflowClick,
        handlePullProjectFromGitClick,
        handleUpdateProjectGitConfigurationSubmit,
        projectGitConfiguration,
        projectVersions,
    };
};
