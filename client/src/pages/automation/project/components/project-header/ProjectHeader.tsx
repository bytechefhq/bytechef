import {ButtonGroup} from '@/components/ui/button-group';
import useOpenSavedProjectDeployment from '@/pages/automation/project-deployments/hooks/useOpenSavedProjectDeployment';
import ProjectDeploymentsSheet from '@/pages/automation/project/components/ProjectDeploymentsSheet';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import OutputPanelButton from '@/pages/automation/project/components/project-header/components/OutputButton';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectBreadcrumbSkeleton from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumbSkeleton';
import ProjectDeploymentsButton from '@/pages/automation/project/components/project-header/components/ProjectDeploymentsButton';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {useProjectHeader} from '@/pages/automation/project/components/project-header/hooks/useProjectHeader';
import useProjectHeaderDialogsStore from '@/pages/automation/project/stores/useProjectHeaderDialogsStore';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowActionsButton from '@/pages/platform/workflow-editor/components/WorkflowActionsButton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import PublishPopover from '@/shared/components/PublishPopover';
import useCopilotCompactHeader from '@/shared/components/copilot/hooks/useCopilotCompactHeader';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating} from '@tanstack/react-query';
import {RefObject, useEffect, useSyncExternalStore} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const getOnlineStatus = () => onlineManager.isOnline();

const subscribeToOnlineStatus = (onOnlineStatusChange: () => void) => onlineManager.subscribe(onOnlineStatusChange);

interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    embedded?: boolean;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    embedded,
    projectId,
    projectWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: ProjectHeaderProps) => {
    const copilotLayoutShifted = useCopilotLayoutShifted();

    useCopilotCompactHeader();
    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore(
        useShallow((state) => ({
            projectLeftSidebarOpen: state.projectLeftSidebarOpen,
            setProjectLeftSidebarOpen: state.setProjectLeftSidebarOpen,
        }))
    );
    const {
        deployDialogOpen,
        deploymentsSheetState: projectDeploymentsSheetState,
        publishPopoverOpen,
        setDeployDialogOpen,
        setDeploymentsSheetState: setProjectDeploymentsSheetState,
        setPublishPopoverOpen,
    } = useProjectHeaderDialogsStore(
        useShallow((state) => ({
            deployDialogOpen: state.deployDialogOpen,
            deploymentsSheetState: state.deploymentsSheetState,
            publishPopoverOpen: state.publishPopoverOpen,
            setDeployDialogOpen: state.setDeployDialogOpen,
            setDeploymentsSheetState: state.setDeploymentsSheetState,
            setPublishPopoverOpen: state.setPublishPopoverOpen,
        }))
    );
    const {workflowIsRunning} = useWorkflowEditorStore(
        useShallow((state) => ({
            workflowIsRunning: state.workflowIsRunning,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const openSavedProjectDeployment = useOpenSavedProjectDeployment({
        onDevelopmentDeploymentSaved: (projectDeploymentId) =>
            setProjectDeploymentsSheetState({expandedProjectDeploymentId: projectDeploymentId}),
        projectId,
    });

    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
    const {
        handleProjectValueChange,
        handleProjectWorkflowValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        hasUnpublishedChanges,
        project,
        projectWorkflows,
        publishProjectMutationIsPending,
        workspaceProjects,
    } = useProjectHeader({
        bottomResizablePanelRef,
        chatTrigger,
        projectId,
    });

    useEffect(
        () => () => {
            setDeployDialogOpen(false);
            setProjectDeploymentsSheetState(undefined);
            setPublishPopoverOpen(false);
        },
        [projectId, projectWorkflowId, setDeployDialogOpen, setProjectDeploymentsSheetState, setPublishPopoverOpen]
    );

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator className="shrink-0" isFetching={isSaving} isOnline={isOnline} showLabel />
    );

    if (!project) {
        return <ProjectSkeleton />;
    }

    return (
        <header
            className={twMerge(
                'flex items-center justify-between bg-surface-main px-3 py-2.5 transition-[padding] duration-300 ease-in-out',
                !embedded && projectLeftSidebarOpen && 'pr-3 pl-0',
                !embedded && copilotLayoutShifted && 'pr-0'
            )}
        >
            <div className="flex min-w-0 items-center gap-2">
                <LeftSidebarButton
                    onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)}
                    tooltip="See projects"
                />

                {projectWorkflows ? (
                    <ProjectBreadcrumb
                        itemSelect={
                            <ProjectItemSelect
                                currentLabel={workflow?.label}
                                currentProjectWorkflowId={projectWorkflowId}
                                onWorkflowValueChange={handleProjectWorkflowValueChange}
                                projectWorkflows={projectWorkflows}
                            />
                        }
                        onProjectValueChange={handleProjectValueChange}
                        project={project}
                        projects={workspaceProjects}
                    />
                ) : (
                    <ProjectBreadcrumbSkeleton />
                )}

                {loadingIndicator}
            </div>

            <div className="flex shrink-0 items-center gap-1">
                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <ButtonGroup>
                    <PublishPopover
                        disabled={!hasUnpublishedChanges}
                        isPending={publishProjectMutationIsPending}
                        onOpenChange={setPublishPopoverOpen}
                        onPublishSubmit={handlePublishProjectSubmit}
                        open={publishPopoverOpen}
                        title="Publish Project"
                        tooltip="Publish the project"
                    />

                    <DeployButton
                        onDeploymentSaved={openSavedProjectDeployment}
                        onOpenChange={setDeployDialogOpen}
                        open={deployDialogOpen}
                        project={project}
                    />

                    <ProjectDeploymentsButton onClick={() => setProjectDeploymentsSheetState({})} />
                </ButtonGroup>

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <SettingsMenu
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    project={project}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflow={workflow}
                />
            </div>

            {projectDeploymentsSheetState && (
                <ProjectDeploymentsSheet
                    expandedProjectDeploymentId={projectDeploymentsSheetState.expandedProjectDeploymentId}
                    onSheetOpenChange={(open) => !open && setProjectDeploymentsSheetState(undefined)}
                    project={project}
                    sheetOpen
                />
            )}
        </header>
    );
};

export default ProjectHeader;
