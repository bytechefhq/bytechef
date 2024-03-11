import {Sheet, SheetContent} from '@/components/ui/sheet';
import {useGetWorkflowExecutionQuery} from '@/queries/automation/workflowExecutions.queries';

import useWorkflowExecutionDetailsDialogStore from '../stores/useWorkflowExecutionDetailsDialogStore';
import WorkflowExecutionDetailsAccordion from './WorkflowExecutionDetailsAccordion';
import WorkflowExecutionDetailsSheetWorkflowView from './WorkflowExecutionDetailsSheetWorkflowView';

const WorkflowExecutionDetailsSheet = () => {
    const {setWorkflowExecutionDetailsDialogOpen, workflowExecutionDetailsDialogOpen, workflowExecutionId} =
        useWorkflowExecutionDetailsDialogStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionDetailsDialogOpen
    );

    return (
        <Sheet
            onOpenChange={() => setWorkflowExecutionDetailsDialogOpen(!workflowExecutionDetailsDialogOpen)}
            open={workflowExecutionDetailsDialogOpen}
        >
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-xl">
                {workflowExecutionLoading && <span>Loading...</span>}

                <div className="flex min-w-[500px] max-w-[500px] flex-col border-r border-gray-100 bg-white">
                    {workflowExecution?.job && (
                        <WorkflowExecutionDetailsAccordion
                            job={workflowExecution.job}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </div>

                {workflowExecution && (
                    <WorkflowExecutionDetailsSheetWorkflowView workflowExecution={workflowExecution} />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionDetailsSheet;
