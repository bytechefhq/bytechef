import {Sheet, SheetContent} from '@/components/ui/sheet';
import WorkflowExecutionSheetWorkflowPanel from '@/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetWorkflowPanel';
import {useGetWorkflowExecutionQuery} from '@/shared/queries/embedded/integrationWorkflowExecutions.queries';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetAccordion from './WorkflowExecutionSheetAccordion';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetWorkflowExecutionQuery(
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

                <div className="flex w-7/12 flex-col border-r border-muted bg-white">
                    {workflowExecution?.job && (
                        <WorkflowExecutionSheetAccordion
                            job={workflowExecution.job}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </div>

                {workflowExecution && <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
