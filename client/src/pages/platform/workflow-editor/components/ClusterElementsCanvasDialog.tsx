import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import ClusterElementsWorkflowEditor from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor';
import {DataPillPanelSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {XIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';

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
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);

    const handleOpenChange = (isOpen: boolean) => {
        onOpenChange(isOpen);

        if (!isOpen) {
            useWorkflowNodeDetailsPanelStore.getState().reset();
        }
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogHeader>
                <DialogTitle className="sr-only"></DialogTitle>

                <DialogDescription />
            </DialogHeader>

            <DialogContent className="absolute bottom-4 left-16 top-12 h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 gap-2 bg-surface-main p-0">
                <ClusterElementsWorkflowEditor />

                <WorkflowNodeDetailsPanel
                    className="fixed inset-y-0 right-0 rounded-l-none"
                    closeButton={
                        <DialogClose asChild>
                            <Button icon={<XIcon />} size="icon" title="Close the canvas" variant="ghost" />
                        </DialogClose>
                    }
                    invalidateWorkflowQueries={invalidateWorkflowQueries}
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={workflowNodeOutputs}
                />

                {dataPillPanelOpen && (
                    <Suspense fallback={<DataPillPanelSkeleton />}>
                        <DataPillPanel
                            className="fixed inset-y-0 right-[465px] rounded-none"
                            previousComponentDefinitions={previousComponentDefinitions}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />
                    </Suspense>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ClusterElementsCanvasDialog;
