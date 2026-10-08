import Badge from '@/components/Badge/Badge';
import {ButtonGroup} from '@/components/ui/button-group';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import LeftSidebarButton from '@/pages/automation/project/components/project-header/components/LeftSidebarButton';
import OutputPanelButton from '@/pages/automation/project/components/project-header/components/OutputButton';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import WorkflowActionsButton from '@/pages/automation/project/components/project-header/components/WorkflowActionsButton';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {useProjectHeader} from '@/pages/automation/project/components/project-header/hooks/useProjectHeader';
import {useProjectWorkflowViewOnlyNotice} from '@/pages/automation/project/hooks/useProjectWorkflowViewOnlyNotice';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating} from '@tanstack/react-query';
import {EyeIcon} from 'lucide-react';
import {RefObject, useSyncExternalStore} from 'react';
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

    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
    const readOnly = useWorkflowEditorReadOnly();
    const viewOnlyNoticeVisible = useProjectWorkflowViewOnlyNotice();
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

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator className="size-6 rounded-full" isFetching={isSaving} isOnline={isOnline} />
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
            <div className="flex items-center gap-2">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)} />

                {projectWorkflows && (
                    <ProjectBreadcrumb
                        itemSelect={
                            <ProjectItemSelect
                                currentLabel={workflow?.label}
                                currentProjectWorkflowId={projectWorkflowId}
                                onWorkflowValueChange={handleProjectWorkflowValueChange}
                                projectWorkflows={projectWorkflows}
                            />
                        }
                        project={project}
                    />
                )}

                {loadingIndicator}

                {readOnly && viewOnlyNoticeVisible && (
                    <Badge
                        aria-label="View only"
                        className="ml-3"
                        icon={<EyeIcon />}
                        role="status"
                        styleType="secondary-outline"
                        title="You can view this workflow but not change or run it"
                    >
                        View only
                    </Badge>
                )}
            </div>

            <div className="flex items-center gap-1">
                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    readOnly={readOnly}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <ButtonGroup>
                    <PublishPopover
                        disabled={!hasUnpublishedChanges}
                        isPending={publishProjectMutationIsPending}
                        onPublishProjectSubmit={handlePublishProjectSubmit}
                    />

                    <DeployButton project={project} />
                </ButtonGroup>

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <SettingsMenu
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    project={project}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflow={workflow}
                />
            </div>
        </header>
    );
};

export default ProjectHeader;
