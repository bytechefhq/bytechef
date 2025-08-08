import {Sheet, SheetContent} from '@/components/ui/sheet';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {Suspense, lazy} from 'react';

import {WorkflowCodeEditorSheetSkeleton} from './WorkflowEditorSkeletons';

const WorkflowCodeEditorSheetContent = lazy(() => import('./WorkflowCodeEditorSheetContent'));

interface WorkflowCodeEditorSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenClose: (open: boolean) => void;
    runDisabled: boolean;
    sheetOpen: boolean;
    testConfigurationDisabled: boolean;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowCodeEditorSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenClose,
    runDisabled,
    sheetOpen,
    testConfigurationDisabled,
    workflow,
    workflowTestConfiguration,
}: WorkflowCodeEditorSheetProps) => (
    <Sheet onOpenChange={onSheetOpenClose} open={sheetOpen}>
        <SheetContent
            className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <Suspense fallback={<WorkflowCodeEditorSheetSkeleton />}>
                <WorkflowCodeEditorSheetContent
                    invalidateWorkflowQueries={invalidateWorkflowQueries}
                    runDisabled={runDisabled}
                    testConfigurationDisabled={testConfigurationDisabled}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            </Suspense>
        </SheetContent>
    </Sheet>
);

export default WorkflowCodeEditorSheet;
