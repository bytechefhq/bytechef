import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ClusterElementsWorkflowEditor from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor';
import {DataPillPanelSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import useClusterElementsCanvasDialog from '@/pages/platform/workflow-editor/components/hooks/useClusterElementsCanvasDialog';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {SparklesIcon, XIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPillPanel = lazy(() => import('./datapills/DataPillPanel'));

interface ClusterElementsCanvasDialogProps {
    invalidateWorkflowQueries: () => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
    previousComponentDefinitions: ComponentDefinitionBasic[];
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}

const ClusterElementsCanvasDialog = ({
    invalidateWorkflowQueries,
    onOpenChange,
    open,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: ClusterElementsCanvasDialogProps) => {
    const {copilotEnabled, handleCopilotClick, handleCopilotClose, handleOpenChange} = useClusterElementsCanvasDialog({
        onOpenChange,
    });

    const copilotPanelOpen = useClusterElementsCanvasDialogStore((state) => state.copilotPanelOpen);
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);

    const ff_4070 = useFeatureFlagsStore()('ff-4070');

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogHeader>
                <DialogTitle className="sr-only"></DialogTitle>

                <DialogDescription />
            </DialogHeader>

            <DialogContent className="absolute bottom-4 left-16 top-12 h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 gap-2 bg-surface-main p-0">
                <ClusterElementsWorkflowEditor />

                <WorkflowNodeDetailsPanel
                    className={twMerge(
                        'fixed inset-y-0 right-0 rounded-l-none',
                        copilotPanelOpen && 'right-[450px] rounded-r-none border-r-0'
                    )}
                    closeButton={
                        <div className="flex items-center gap-1">
                            {ff_4070 && copilotEnabled && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            className="[&_svg]:size-5"
                                            icon={<SparklesIcon />}
                                            onClick={handleCopilotClick}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Open Copilot panel</TooltipContent>
                                </Tooltip>
                            )}

                            <DialogClose asChild>
                                <Button icon={<XIcon />} size="icon" title="Close the canvas" variant="ghost" />
                            </DialogClose>
                        </div>
                    }
                    invalidateWorkflowQueries={invalidateWorkflowQueries}
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={workflowNodeOutputs}
                />

                {dataPillPanelOpen && (
                    <Suspense fallback={<DataPillPanelSkeleton />}>
                        <DataPillPanel
                            className={twMerge(
                                'fixed inset-y-0 right-[465px] rounded-none',
                                copilotPanelOpen && 'right-[915px]'
                            )}
                            previousComponentDefinitions={previousComponentDefinitions}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />
                    </Suspense>
                )}

                <CopilotPanel
                    className={twMerge(
                        'rounded-2 fixed inset-y-0 right-0 rounded-r-md border-l',
                        !copilotPanelOpen && 'hidden'
                    )}
                    onClose={handleCopilotClose}
                />
            </DialogContent>
        </Dialog>
    );
};

export default ClusterElementsCanvasDialog;
