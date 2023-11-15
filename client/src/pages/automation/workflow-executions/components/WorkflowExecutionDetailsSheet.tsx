import {Sheet, SheetContent} from '@/components/ui/sheet';
import WorkflowExecutionsDetailsAccordion from '@/pages/automation/workflow-executions/components/WorkflowExecutionsDetailsAccordion';
import {useGetWorkflowExecutionQuery} from '@/queries/workflowExecutions.queries';

import useWorkflowExecutionDetailsDialogStore from '..//stores/useWorkflowExecutionDetailsDialogStore';
import WorkflowExecutionsWorkflowView from './WorkflowExecutionsWorkflowView';

const WorkflowExecutionDetailsSheet = () => {
    const {
        setWorkflowExecutionDetailsDialogOpen,
        workflowExecutionDetailsDialogOpen,
        workflowExecutionId,
    } = useWorkflowExecutionDetailsDialogStore();

    const {data: workflowExecution, isLoading: workflowExecutionLoading} =
        useGetWorkflowExecutionQuery(
            {
                id: workflowExecutionId,
            },
            workflowExecutionDetailsDialogOpen
        );

    return (
        <Sheet
            onOpenChange={() =>
                setWorkflowExecutionDetailsDialogOpen(
                    !workflowExecutionDetailsDialogOpen
                )
            }
            open={workflowExecutionDetailsDialogOpen}
        >
            <SheetContent className="flex w-11/12 p-0 sm:max-w-[1280px]">
                {workflowExecutionLoading && <span>Loading...</span>}

                <div className="flex w-7/12 flex-col border-r border-gray-100 bg-white">
                    {workflowExecution && (
                        <WorkflowExecutionsDetailsAccordion
                            workflowExecution={workflowExecution}
                        />
                    )}
                </div>

                {workflowExecution && (
                    <WorkflowExecutionsWorkflowView
                        workflowExecution={workflowExecution}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionDetailsSheet;
