import PageLoader from '@/components/PageLoader';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {ReactFlowProvider} from '@xyflow/react';
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
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-workflow-read-only-project-deployment-workflow-sheet-width">
                <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                    <SheetTitle>{workflow?.label}</SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                <ReactFlowProvider>
                    <PageLoader
                        errors={[componentsError, taskDispatcherDefinitionsError]}
                        loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
                    >
                        {componentDefinitions && taskDispatcherDefinitions && workflow && (
                            <Suspense>
                                <WorkflowEditor
                                    componentDefinitions={componentDefinitions}
                                    invalidateWorkflowQueries={() => {}}
                                    readOnlyWorkflow={workflow}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                />
                            </Suspense>
                        )}
                    </PageLoader>
                </ReactFlowProvider>
            </SheetContent>
        </Sheet>
    );
};

export default ReadOnlyWorkflowSheet;
