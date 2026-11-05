import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import {Skeleton} from '@/components/ui/skeleton';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectDeploymentList from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import useOpenSavedProjectDeployment from '@/pages/automation/project-deployments/hooks/useOpenSavedProjectDeployment';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AlertTriangleIcon, Layers3Icon, RocketIcon, SendIcon} from 'lucide-react';
import {useMemo} from 'react';

interface ProjectDeploymentsPanelProps {
    enabled: boolean;
    expandAll?: boolean;
    expandedProjectDeploymentId?: number;
    onProjectDeploymentSaved: (projectDeploymentId: number) => void;
    project: Project;
    showEnvironment?: boolean;
}

const ProjectDeploymentsPanel = ({
    enabled,
    expandAll = false,
    expandedProjectDeploymentId,
    onProjectDeploymentSaved,
    project,
    showEnvironment = false,
}: ProjectDeploymentsPanelProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const openSavedProjectDeployment = useOpenSavedProjectDeployment({
        onDevelopmentDeploymentSaved: onProjectDeploymentSaved,
        projectId: project.id,
    });

    const {
        data: projectDeployments,
        error: projectDeploymentsError,
        isLoading: projectDeploymentsIsLoading,
        refetch: refetchProjectDeployments,
    } = useGetWorkspaceProjectDeploymentsQuery(
        {
            id: currentWorkspaceId ?? 0,
            projectId: project.id ?? 0,
        },
        enabled && !!currentWorkspaceId && !!project.id
    );

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
            triggerDefinitions: true,
        },
        enabled
    );

    const {data: tags} = useGetProjectDeploymentTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const environmentProjectDeployments = useMemo(
        () =>
            (projectDeployments ?? [])
                .filter((projectDeployment) => projectDeployment.environmentId === currentEnvironmentId)
                .sort((deployment, otherDeployment) => deployment.name.localeCompare(otherDeployment.name)),
        [currentEnvironmentId, projectDeployments]
    );

    const isPublished = !!(project.lastPublishedDate && project.lastProjectVersion);

    const projectDeploymentsLoadFailed = !!projectDeploymentsError && projectDeployments === undefined;

    const knownProjectDeploymentCount =
        currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? project.developmentDeploymentCount : undefined;

    const skeletonProjectDeploymentCount = Math.min(knownProjectDeploymentCount ?? 1, 3);

    return (
        <>
            {projectDeploymentsIsLoading && knownProjectDeploymentCount === 0 && (
                <div className="flex items-center justify-center py-6">
                    <LoadingIcon />
                </div>
            )}

            {projectDeploymentsIsLoading && knownProjectDeploymentCount !== 0 && (
                <ul aria-busy="true" aria-label="Loading deployments">
                    {Array.from({length: skeletonProjectDeploymentCount}, (_, index) => (
                        <li
                            className="mb-2 flex items-center justify-between rounded border border-border/50 p-3"
                            key={index}
                        >
                            <div className="space-y-2">
                                <Skeleton className="h-5 w-40" />

                                <Skeleton className="h-4 w-24" />
                            </div>

                            <div className="flex items-center gap-4">
                                <Skeleton className="h-6 w-9 rounded-full" />

                                <Skeleton className="h-6 w-11 rounded-full" />
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {projectDeploymentsLoadFailed && (
                <EmptyState
                    actions={
                        <Button label="Retry" onClick={() => void refetchProjectDeployments()} variant="outline" />
                    }
                    description="The deployments of this project could not be loaded. Please try again."
                    icon={<AlertTriangleIcon />}
                    title="Failed to load deployments"
                />
            )}

            {!projectDeploymentsIsLoading &&
                !projectDeploymentsLoadFailed &&
                environmentProjectDeployments.length === 0 &&
                (isPublished ? (
                    <EmptyState
                        actions={
                            <ProjectDeploymentDialog
                                environmentEditable
                                onSuccess={openSavedProjectDeployment}
                                projectDeployment={{
                                    environmentId: currentEnvironmentId,
                                    name: project.name,
                                    projectId: project.id,
                                }}
                                projectDeployments={projectDeployments}
                                redirectOnSubmit={false}
                                showTabs
                                triggerNode={<Button icon={<RocketIcon />} label="Deploy" />}
                            />
                        }
                        description="Deploy this project to run its workflows here."
                        icon={<Layers3Icon />}
                        title={`No deployments in ${ENVIRONMENT_CONFIGS[currentEnvironmentId]?.label ?? 'this environment'}`}
                    />
                ) : (
                    <EmptyState
                        description="Deployments run a published, read-only version of a project. Publish this project to create its first version."
                        icon={<SendIcon />}
                        title="Publish the project to deploy it"
                    />
                ))}

            {environmentProjectDeployments.length > 0 && (
                <WorkflowReadOnlyProvider
                    value={{
                        useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                    }}
                >
                    <ProjectDeploymentList
                        componentDefinitions={componentDefinitions}
                        expandAll={expandAll}
                        hideOpenInProject
                        newlyCreatedDeploymentId={expandedProjectDeploymentId}
                        project={project}
                        projectDeployments={environmentProjectDeployments}
                        showEnvironment={showEnvironment}
                        showTags={false}
                        tags={tags ?? []}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />

                    <ProjectDeploymentWorkflowExecutionsSheet />
                </WorkflowReadOnlyProvider>
            )}
        </>
    );
};

export default ProjectDeploymentsPanel;
