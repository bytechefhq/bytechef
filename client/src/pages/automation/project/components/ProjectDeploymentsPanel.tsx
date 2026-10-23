import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectDeploymentList from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import useOpenSavedProjectDeployment from '@/pages/automation/project-deployments/hooks/useOpenSavedProjectDeployment';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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

    const canCreateDeployment = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');

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

    const {data: tags} = useGetProjectDeploymentTagsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
    });

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

    return (
        <>
            {projectDeploymentsIsLoading && (
                <div className="flex items-center justify-center py-6">
                    <LoadingIcon />
                </div>
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
                            canCreateDeployment && (
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
                            )
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
