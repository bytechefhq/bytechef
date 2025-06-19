import {Sheet, SheetContent} from '@/components/ui/sheet';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetAccordion from './WorkflowExecutionSheetAccordion';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionDetailsSheetOpen, workflowExecutionDetailsSheetOpen, workflowExecutionId} =
        useWorkflowExecutionSheetStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionDetailsSheetOpen
    );

    return (
        <Sheet
            onOpenChange={() => setWorkflowExecutionDetailsSheetOpen(!workflowExecutionDetailsSheetOpen)}
            open={workflowExecutionDetailsSheetOpen}
        >
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-xl">
                {workflowExecutionLoading && <span>Loading...</span>}

                <div className="flex min-w-workflow-execution-sheet-width max-w-workflow-execution-sheet-width flex-col border-r border-r-border/50 bg-white">
                    {workflowExecution?.job && (
                        <WorkflowExecutionSheetAccordion
                            job={workflowExecution.job}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </div>

                {workflowExecution && (
                    <WorkflowReadOnlyProvider
                        value={{
                            useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                        }}
                    >
                        <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />
                    </WorkflowReadOnlyProvider>
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
