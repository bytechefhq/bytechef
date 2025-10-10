import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import OutputPanelButton from '@/ee/pages/embedded/automation-workflows/workflow-builder/components/workflow-builder-header/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/automation-workflows/workflow-builder/components/workflow-builder-header/components/PublishPopover';
import WorkflowActionsButton from '@/ee/pages/embedded/automation-workflows/workflow-builder/components/workflow-builder-header/components/WorkflowActionsButton';
import {useWorkflowBuilderHeader} from '@/ee/pages/embedded/automation-workflows/workflow-builder/components/workflow-builder-header/hooks/useWorkflowBuilderHeader';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsFetching} from '@tanstack/react-query';
import {EditIcon} from 'lucide-react';
import {RefObject} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useShallow} from 'zustand/react/shallow';
import LoadingIndicator from '@/shared/components/LoadingIndicator';

interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowVersion?: number;
}

const WorkflowBuilderHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    projectId,
    runDisabled,
    updateWorkflowMutation,
    workflowVersion,
}: ProjectHeaderProps) => {
    const {setShowEditWorkflowDialog, showEditWorkflowDialog, workflowIsRunning} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
            showEditWorkflowDialog: state.showEditWorkflowDialog,
            workflowIsRunning: state.workflowIsRunning,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const isFetching = useIsFetching();
    const {
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        publishProjectMutationIsPending,
    } = useWorkflowBuilderHeader({
        bottomResizablePanelRef,
        chatTrigger,
        projectId,
    });

    const isOnline = onlineManager.isOnline();

    // if (!project) {
    //     return <WorkflowBuilderSkeleton />;
    // }

    return (
        <header className="flex items-center justify-between bg-transparent px-3 py-2.5">
            <div className="flex items-center gap-2">
                <div>{workflow.label}</div>

                <div></div>

                <Badge className="flex space-x-1 bg-white" variant="outline">
                    <span>{`V${(workflowVersion ?? 0) + 1} DRAFT`}</span>
                </Badge>
            </div>

            <div className="flex items-center space-x-2">
                <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />

                <Button
                    className="hover:bg-surface-neutral-primary-hover [&_svg]:size-5"
                    onClick={() => setShowEditWorkflowDialog(true)}
                    size="icon"
                    variant="ghost"
                >
                    <EditIcon />
                </Button>

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

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

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectId={projectId}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </header>
    );
};

export default WorkflowBuilderHeader;
