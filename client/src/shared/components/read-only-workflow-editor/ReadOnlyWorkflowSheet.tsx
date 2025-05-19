import PageLoader from '@/components/PageLoader';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowEditor from '@/pages/platform/workflow-editor/components/WorkflowEditor';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {ReactFlowProvider} from '@xyflow/react';

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

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        useUpdateWorkflowMutation,
        workflowId: workflow?.id as string,
        workflowKeys: WorkflowKeys,
    });

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

                <WorkflowMutationProvider
                    value={{
                        updateWorkflowMutation: updateWorkflowEditorMutation,
                    }}
                >
                    <ReactFlowProvider>
                        <PageLoader
                            errors={[componentsError, taskDispatcherDefinitionsError]}
                            loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
                        >
                            {componentDefinitions && taskDispatcherDefinitions && workflow && (
                                <WorkflowEditor
                                    componentDefinitions={componentDefinitions}
                                    readOnlyWorkflow={workflow}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                />
                            )}
                        </PageLoader>
                    </ReactFlowProvider>
                </WorkflowMutationProvider>
            </SheetContent>
        </Sheet>
    );
};

export default ReadOnlyWorkflowSheet;
