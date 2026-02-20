import {Sheet, SheetContent} from '@/components/ui/sheet';
import {WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {Suspense, lazy} from 'react';

import {WorkflowSheetSkeleton} from '../WorkflowEditorSkeletons';

const WorkflowInputsSheetContent = lazy(() => import('./WorkflowInputsSheetContent'));

interface WorkflowInputsSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenChange: (open: boolean) => void;
    sheetOpen: boolean;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowInputsSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenChange,
    sheetOpen,
    workflowTestConfiguration,
}: WorkflowInputsSheetProps) => (
    <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
        <SheetContent
            className="bottom-4 right-4 top-3 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-inputs-sheet-width"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <Suspense fallback={<WorkflowSheetSkeleton title="Workflow Inputs" />}>
                <WorkflowInputsSheetContent
                    invalidateWorkflowQueries={invalidateWorkflowQueries}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            </Suspense>
        </SheetContent>
    </Sheet>
);

export default WorkflowInputsSheet;
