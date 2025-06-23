import {Sheet, SheetContent} from '@/components/ui/sheet';
import WorkflowExecutionSheetWorkflowPanel from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetWorkflowPanel';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationWorkflowExecutionQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetAccordion from './WorkflowExecutionSheetAccordion';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetIntegrationWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionSheetOpen
    );

    return (
        <Sheet
            onOpenChange={() => setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen)}
            open={workflowExecutionSheetOpen}
        >
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-xl">
                {workflowExecutionLoading && <span>Loading...</span>}

                <div className="flex w-7/12 flex-col border-r border-r-border/50 bg-white">
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
