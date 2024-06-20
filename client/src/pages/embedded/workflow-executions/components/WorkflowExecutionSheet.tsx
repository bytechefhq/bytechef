import {Sheet, SheetContent} from '@/components/ui/sheet';
import WorkflowExecutionWorkflowPanel from '@/pages/automation/workflow-executions/components/WorkflowExecutionWorkflowPanel';
import {useGetWorkflowExecutionQuery} from '@/shared/queries/embedded/integrationWorkflowExecutions.queries';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionAccordion from './WorkflowExecutionAccordion';

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

                <div className="flex w-7/12 flex-col border-r border-gray-100 bg-white">
                    {workflowExecution?.job && (
                        <WorkflowExecutionAccordion
                            job={workflowExecution.job}
                            triggerExecution={workflowExecution?.triggerExecution}
                        />
                    )}
                </div>

                {workflowExecution && <WorkflowExecutionWorkflowPanel workflowExecution={workflowExecution} />}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
