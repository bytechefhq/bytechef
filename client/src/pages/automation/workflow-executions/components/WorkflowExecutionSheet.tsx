import {Sheet, SheetContent} from '@/components/ui/sheet';
import {useGetWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionAccordion from './WorkflowExecutionAccordion';
import WorkflowExecutionSheetPanel from './WorkflowExecutionSheetPanel';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionDetailsSheetOpen, workflowExecutionDetailsSheetOpen, workflowExecutionId} =
        useWorkflowExecutionSheetStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetWorkflowExecutionQuery(
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

                <div className="flex min-w-[500px] max-w-[500px] flex-col border-r border-gray-100 bg-white">
                    {workflowExecution?.job && (
                        <WorkflowExecutionAccordion
                            job={workflowExecution.job}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </div>

                {workflowExecution && <WorkflowExecutionSheetPanel workflowExecution={workflowExecution} />}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
