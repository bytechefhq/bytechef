import LeftSidebarButton from '@/pages/automation/project/components/project-header/components/LeftSidebarButton';
import LoaderNotification from '@/pages/automation/project/components/project-header/components/LoaderNotification';
import OutputPanelButton from '@/pages/automation/project/components/project-header/components/OutputButton';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import WorkflowActionsButton from '@/pages/automation/project/components/project-header/components/WorkflowActionsButton';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {useProjectHeader} from '@/pages/automation/project/components/project-header/hooks/useProjectHeader';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsFetching} from '@tanstack/react-query';
import {RefObject} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';

const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    projectId,
    projectWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) => {
    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore();
    const {workflowIsRunning} = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();

    const isFetching = useIsFetching();
    const {
        handleProjectWorkflowValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
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
        <header className="flex items-center justify-between bg-surface-main px-3 py-2.5">
            <div className="flex items-center">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)} />

                {projectWorkflows && (
                    <ProjectBreadcrumb
                        currentWorkflow={workflow}
                        onProjectWorkflowValueChange={handleProjectWorkflowValueChange}
                        project={project}
                        projectWorkflowId={projectWorkflowId}
                        projectWorkflows={projectWorkflows}
                    />
                )}
            </div>

            <div className="flex items-center space-x-2">
                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <LoaderNotification isFetching={isFetching} isOnline={isOnline} />

                <SettingsMenu project={project} updateWorkflowMutation={updateWorkflowMutation} workflow={workflow} />

                <PublishPopover
                    isPending={publishProjectMutationIsPending}
                    onPublishProjectSubmit={handlePublishProjectSubmit}
                />

                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />
            </div>
        </header>
    );
};

export default ProjectHeader;
