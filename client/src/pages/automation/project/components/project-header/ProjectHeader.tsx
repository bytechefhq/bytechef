import {ButtonGroup} from '@/components/ui/button-group';
import {Separator} from '@/components/ui/separator';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import LeftSidebarButton from '@/pages/automation/project/components/project-header/components/LeftSidebarButton';
import OutputPanelButton from '@/pages/automation/project/components/project-header/components/OutputButton';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import WorkflowActionsButton from '@/pages/automation/project/components/project-header/components/WorkflowActionsButton';
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {useProjectHeader} from '@/pages/automation/project/components/project-header/hooks/useProjectHeader';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsFetching} from '@tanstack/react-query';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    embedded?: boolean;
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    embedded,
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    runDisabled,
    showPublishDeploy,
    showWorkflowSelect,
    updateWorkflowMutation,
}: ProjectHeaderProps) => {
    const copilotLayoutShifted = useCopilotLayoutShifted();
    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore(
        useShallow((state) => ({
            projectLeftSidebarOpen: state.projectLeftSidebarOpen,
            setProjectLeftSidebarOpen: state.setProjectLeftSidebarOpen,
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

    const isFetching = useIsFetching();
    const {
        handleProjectWorkflowValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        hasUnpublishedChanges,
        project,
        projectWorkflows,
        publishProjectMutationIsPending,
    } = useProjectHeader({
        bottomResizablePanelRef,
        chatTrigger,
        projectId,
    });

    const isOnline = onlineManager.isOnline();

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
            <div className="flex items-center">
                {/* The embedded AI Hub workflow editor opens each workflow as its own resource-panel tab and
                 * has no project tree, so the breadcrumb, workflow selector, and the project-sidebar toggle
                 * are all redundant there — hidden behind `embedded`. The full-screen Project page keeps them. */}

                {!embedded && (
                    <>
                        <LeftSidebarButton
                            onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)}
                        />

                        <Separator className="mr-4 ml-2 h-4" orientation="vertical" />

                        {projectWorkflows && (
                            <ProjectBreadcrumb
                                currentWorkflow={workflow}
                                onProjectWorkflowValueChange={handleProjectWorkflowValueChange}
                                project={project}
                                projectWorkflowId={projectWorkflowId}
                                projectWorkflows={projectWorkflows}
                            />
                        )}
                    </>
                )}

                {embedded && showWorkflowSelect && projectWorkflows && (
                    <WorkflowSelect
                        currentWorkflowLabel={workflow?.label}
                        onValueChange={onWorkflowChange ?? handleProjectWorkflowValueChange}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                        projectWorkflows={projectWorkflows}
                    />
                )}
            </div>

            <div className="flex items-center">
                <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />

                {!embedded && (
                    <SettingsMenu
                        project={project}
                        updateWorkflowMutation={updateWorkflowMutation}
                        workflow={workflow}
                    />
                )}

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                {!embedded && (
                    <ButtonGroup>
                        <PublishPopover
                            disabled={!hasUnpublishedChanges}
                            isPending={publishProjectMutationIsPending}
                            onPublishProjectSubmit={handlePublishProjectSubmit}
                        />

                        <DeployButton project={project} />
                    </ButtonGroup>
                )}

                {embedded && showPublishDeploy && (
                    <ButtonGroup>
                        <PublishPopover
                            disabled={!hasUnpublishedChanges}
                            isPending={publishProjectMutationIsPending}
                            onPublishProjectSubmit={handlePublishProjectSubmit}
                        />

                        <DeployButton project={project} />
                    </ButtonGroup>
                )}
            </div>
        </header>
    );
};

export default ProjectHeader;
