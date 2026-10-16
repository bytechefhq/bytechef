import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {HubBuilderContext} from '@/ee/pages/embedded/automation-hub/hubBuilderContext';
import OutputPanelButton from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/PublishPopover';
import WorkflowActionsButton from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/WorkflowActionsButton';
import WorkflowBuilderSettingsMenu from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/components/WorkflowBuilderSettingsMenu';
import {useWorkflowBuilderHeader} from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/hooks/useWorkflowBuilderHeader';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating, useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {RefObject, useContext, useSyncExternalStore} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useShallow} from 'zustand/react/shallow';

const getOnlineStatus = () => onlineManager.isOnline();

const subscribeToOnlineStatus = (onOnlineStatusChange: () => void) => onlineManager.subscribe(onOnlineStatusChange);

interface ProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
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

    const queryClient = useQueryClient();

    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
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

    const hubBuilderContext = useContext(HubBuilderContext);

    // if (!project) {
    //     return <WorkflowBuilderSkeleton />;
    // }

    return (
        <header className="flex items-center justify-between bg-transparent px-3 py-2.5">
            <div className="flex items-center gap-2">
                {hubBuilderContext?.onBack && (
                    <Button
                        aria-label="Back to automations"
                        className="-ml-2.5"
                        icon={<ArrowLeftIcon />}
                        onClick={hubBuilderContext.onBack}
                        size="icon"
                        variant="ghost"
                    />
                )}

                <div>{workflow.label}</div>

                <Badge label={`V${(workflowVersion ?? 0) + 1} DRAFT`} styleType="outline-outline" weight="semibold" />

                {(isSaving > 0 || !isOnline) && (
                    <LoadingIndicator
                        className="size-6 rounded-full hover:bg-surface-neutral-secondary-hover"
                        isFetching={isSaving}
                        isOnline={isOnline}
                    />
                )}
            </div>

            <div className="flex items-center gap-1">
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

                <OutputPanelButton onShowOutputClick={handleShowOutputClick} />

                <WorkflowBuilderSettingsMenu onEditWorkflowClick={() => setShowEditWorkflowDialog(true)} />
            </div>

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    onSave={() =>
                        queryClient.invalidateQueries({
                            queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, Number.parseInt(workflow.id!, 10)),
                        })
                    }
                    parentId={projectId}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </header>
    );
};

export default WorkflowBuilderHeader;
