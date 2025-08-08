import {Sheet, SheetContent} from '@/components/ui/sheet';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {Suspense, lazy} from 'react';

import {WorkflowSheetSkeleton} from './WorkflowEditorSkeletons';

const WorkflowOutputsSheetContent = lazy(() => import('./WorkflowOutputsSheetContent'));

interface WorkflowOutputsSheetProps {
    onSheetOpenChange: (open: boolean) => void;
    sheetOpen: boolean;
    workflow: Workflow;
}

const WorkflowOutputsSheet = ({onSheetOpenChange, sheetOpen, workflow}: WorkflowOutputsSheetProps) => (
    <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-workflow-outputs-sheet-width"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <Suspense fallback={<WorkflowSheetSkeleton title="Workflow Outputs" />}>
                <WorkflowOutputsSheetContent workflow={workflow} />
            </Suspense>
        </SheetContent>
    </Sheet>
);

export default WorkflowOutputsSheet;
