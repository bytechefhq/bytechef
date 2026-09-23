import PageLoader from '@/components/PageLoader';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WIDTHS} from '@/shared/theme/constants';
import {ReactFlowProvider} from '@xyflow/react';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy} from 'react';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

import useReadOnlyWorkflow from './hooks/useReadOnlyWorkflow';

const ReadOnlyWorkflowSheet = () => {
    const {closeReadOnlyWorkflowSheet, isReadOnlyWorkflowSheetOpen, workflow} = useReadOnlyWorkflow();

    const {
        componentDefinitions,
        componentsError,
        componentsIsLoading,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
    } = useWorkflowLayout();

    return (
        <Sheet
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    closeReadOnlyWorkflowSheet();
                }
            }}
            open={isReadOnlyWorkflowSheetOpen}
        >
            <VisuallyHidden.Root>
                <SheetTitle>{workflow?.label}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-read-only-project-deployment-workflow-sheet-width">
                <header className="flex shrink-0 items-center justify-between rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <span className="text-lg font-semibold">{workflow?.label}</span>

                    <SheetCloseButton />
                </header>

                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg">
                    <ReactFlowProvider>
                        <PageLoader
                            errors={[componentsError, taskDispatcherDefinitionsError]}
                            loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
                        >
                            {componentDefinitions && taskDispatcherDefinitions && workflow && (
                                <Suspense>
                                    <WorkflowEditor
                                        componentDefinitions={componentDefinitions}
                                        customCanvasWidth={WIDTHS.WORKFLOW_READ_ONLY_SHEET_CANVAS_WIDTH}
                                        readOnlyWorkflow={workflow}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </Suspense>
                            )}
                        </PageLoader>
                    </ReactFlowProvider>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default ReadOnlyWorkflowSheet;
