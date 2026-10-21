import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import ProjectDeploymentList from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ExternalLinkIcon, RocketIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDeploymentsSheetProps {
    environmentId?: number;
    expandedProjectDeploymentId?: number;
    onSheetOpenChange: (open: boolean) => void;
    project: Project;
    sheetOpen: boolean;
}

const ProjectDeploymentsSheet = ({
    environmentId: initialEnvironmentId,
    expandedProjectDeploymentId,
    onSheetOpenChange,
    project,
    sheetOpen,
}: ProjectDeploymentsSheetProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );

    const [environmentId, setEnvironmentId] = useState<number>(initialEnvironmentId ?? currentEnvironmentId);

    const navigate = useNavigate();

    const {data: projectDeployments, isLoading: projectDeploymentsIsLoading} = useGetWorkspaceProjectDeploymentsQuery(
        {
            id: currentWorkspaceId ?? 0,
            projectId: project.id ?? 0,
        },
        sheetOpen && !!currentWorkspaceId && !!project.id
    );

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
            triggerDefinitions: true,
        },
        sheetOpen
    );

    const {data: tags} = useGetProjectDeploymentTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const environmentProjectDeployments = useMemo(
        () =>
            (projectDeployments ?? [])
                .filter((projectDeployment) => projectDeployment.environmentId === environmentId)
                .sort((deployment, otherDeployment) => deployment.name.localeCompare(otherDeployment.name)),
        [environmentId, projectDeployments]
    );

    const handleOpenInDeploymentsClick = () => {
        setCurrentEnvironmentId(environmentId);

        onSheetOpenChange(false);

        navigate(`/automation/deployments?projectId=${project.id}`);
    };

    return (
        <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-deployments-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header
                    className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-sidebar-border bg-sidebar p-3"
                    data-environment={ENVIRONMENT_CONFIGS[environmentId]?.sidebarTheme}
                >
                    <div className="flex items-center gap-x-2">
                        <RocketIcon className="size-5" />

                        <SheetTitle className="text-lg font-semibold">Project Deployments</SheetTitle>
                    </div>

                    <div className="flex items-center gap-x-1">
                        <EnvironmentSelect align="end" environmentId={environmentId} onChange={setEnvironmentId} />

                        <SheetCloseButton />
                    </div>
                </header>

                <div className="min-h-0 flex-1 overflow-y-auto bg-surface-neutral-primary p-3">
                    {projectDeploymentsIsLoading && (
                        <div className="flex items-center justify-center py-6">
                            <LoadingIcon />
                        </div>
                    )}

                    {!projectDeploymentsIsLoading && environmentProjectDeployments.length === 0 && (
                        <p className="py-6 text-center text-sm text-muted-foreground">
                            No deployments in this environment.
                        </p>
                    )}

                    {environmentProjectDeployments.length > 0 && (
                        <WorkflowReadOnlyProvider
                            value={{
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                            }}
                        >
                            <ProjectDeploymentList
                                componentDefinitions={componentDefinitions}
                                hideOpenInProject
                                newlyCreatedDeploymentId={expandedProjectDeploymentId}
                                project={project}
                                projectDeployments={environmentProjectDeployments}
                                showEnvironment
                                tags={tags ?? []}
                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                            />

                            <ProjectDeploymentWorkflowExecutionsSheet />
                        </WorkflowReadOnlyProvider>
                    )}
                </div>

                <footer className="flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Close" onClick={() => onSheetOpenChange(false)} variant="outline" />

                    <Button
                        icon={<ExternalLinkIcon />}
                        label="Open in Deployments"
                        onClick={handleOpenInDeploymentsClick}
                    />
                </footer>
            </SheetContent>
        </Sheet>
    );
};

export default ProjectDeploymentsSheet;
