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
            className="flex flex-col p-4 sm:max-w-workflow-inputs-sheet-width"
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
