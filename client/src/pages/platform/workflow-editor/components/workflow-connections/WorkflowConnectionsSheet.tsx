import {Sheet, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy} from 'react';

import {WorkflowSheetSkeleton} from '../WorkflowEditorSkeletons';
import {useSaveWorkflowConnectionsStore} from './hooks/useSaveWorkflowConnections';

const WorkflowConnectionsSheetContent = lazy(() => import('./WorkflowConnectionsSheetContent'));

export interface WorkflowConnectionsSheetProps {
    onSheetOpenChange: (open: boolean) => void;
    sheetOpen: boolean;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
    workflowTestConfigurationLoading?: boolean;
}

const WorkflowConnectionsSheet = ({
    onSheetOpenChange,
    sheetOpen,
    workflow,
    workflowTestConfiguration,
    workflowTestConfigurationLoading,
}: WorkflowConnectionsSheetProps) => {
    const savePending = useSaveWorkflowConnectionsStore((state) => state.pendingBatchCount > 0);

    const handleOpenChange = (open: boolean) => {
        // Closing mid-save would let a canvas edit re-save the test configuration while the batch still writes it.
        if (!open && savePending) {
            return;
        }

        onSheetOpenChange(open);
    };

    return (
        <Sheet onOpenChange={handleOpenChange} open={sheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>Workflow Connections</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-connections-sheet-width"
                onEscapeKeyDown={(event) => {
                    if (savePending) {
                        event.preventDefault();
                    }
                }}
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <Suspense fallback={<WorkflowSheetSkeleton title="Workflow Connections" />}>
                    <WorkflowConnectionsSheetContent
                        workflow={workflow}
                        workflowTestConfiguration={workflowTestConfiguration}
                        workflowTestConfigurationLoading={workflowTestConfigurationLoading}
                    />
                </Suspense>
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowConnectionsSheet;
