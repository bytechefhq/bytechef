import {Separator} from '@/components/ui/separator';
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
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    projectId,
    projectWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: ProjectHeaderProps) => {
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
                'flex items-center justify-between bg-surface-main px-3 py-2.5',
                projectLeftSidebarOpen && 'pl-0 pr-3'
            )}
        >
            <div className="flex items-center">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)} />

                <Separator className="ml-2 mr-4 h-4" orientation="vertical" />

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

            <div className="flex items-center">
                <LoaderNotification isFetching={isFetching} isOnline={isOnline} />

                <SettingsMenu project={project} updateWorkflowMutation={updateWorkflowMutation} workflow={workflow} />

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <PublishPopover
                    isPending={publishProjectMutationIsPending}
                    onPublishProjectSubmit={handlePublishProjectSubmit}
                />
            </div>
        </header>
    );
};

export default ProjectHeader;
