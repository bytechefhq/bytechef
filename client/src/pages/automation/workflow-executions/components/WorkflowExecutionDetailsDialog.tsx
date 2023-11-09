import {Accordion} from '@/components/ui/accordion';
import WorkflowTriggerAccordion from '@/pages/automation/workflow-executions/components/dialog-content/WorkflowTriggerAccordion';
import {useGetWorkflowExecutionQuery} from '@/queries/executions';
import * as Dialog from '@radix-ui/react-dialog';
import {CheckCircledIcon} from '@radix-ui/react-icons';
import {twMerge} from 'tailwind-merge';

import useWorkflowExecutionDetailsDialogStore from '..//stores/useWorkflowExecutionDetailsDialogStore';
import ReadOnlyWorkflow from './dialog-content/ReadOnlyWorkflow';
import WorkflowTaskListAccordion from './dialog-content/WorkflowTaskListAccordion';

const WorkflowExecutionDetailsDialog = () => {
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

    const taskExecutionsCompleted =
        workflowExecution?.job?.taskExecutions?.every(
            (taskExecution) => taskExecution.status === 'COMPLETED'
        );
    const triggerExecutionCompleted =
        workflowExecution?.triggerExecution?.status === 'COMPLETED';

    const startTime = workflowExecution?.job?.startDate?.getTime();
    const endTime = workflowExecution?.job?.endDate?.getTime();

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount =
        workflowExecution?.job?.taskExecutions?.length || 0;

    return (
        <Dialog.Root
            modal={false}
            onOpenChange={() =>
                setWorkflowExecutionDetailsDialogOpen(
                    !workflowExecutionDetailsDialogOpen
                )
            }
            open={workflowExecutionDetailsDialogOpen}
        >
            <Dialog.Portal>
                <Dialog.Content className="fixed inset-y-0 right-0 z-10 flex w-full max-w-7xl overflow-hidden border-l border-gray-100 shadow-lg">
                    {workflowExecutionLoading && <span>Loading...</span>}

                    <div className="flex w-7/12 flex-col border-r border-gray-100 bg-white">
                        <Dialog.Title className="px-3 py-4">
                            <div className="mb-3 flex items-center justify-between">
                                <span className="text-lg">
                                    {taskExecutionsCompleted &&
                                    triggerExecutionCompleted
                                        ? 'Workflow executed successfully'
                                        : 'Workflow failed'}
                                </span>

                                <CheckCircledIcon
                                    className={twMerge(
                                        'h-5 w-5',
                                        taskExecutionsCompleted &&
                                            triggerExecutionCompleted
                                            ? 'text-green-500'
                                            : 'text-red-500'
                                    )}
                                />
                            </div>

                            <div className="flex justify-between text-xs">
                                <span>
                                    {workflowExecution?.job?.startDate &&
                                        `${workflowExecution?.job?.startDate?.toLocaleDateString()} ${workflowExecution?.job?.startDate?.toLocaleTimeString()}`}
                                </span>

                                <span>Duration: {duration}ms</span>

                                <span>
                                    {`${taskExecutionsCount} task${
                                        taskExecutionsCount > 1 ? 's' : ''
                                    } executed`}
                                </span>
                            </div>
                        </Dialog.Title>

                        {!!workflowExecution?.job?.taskExecutions?.length && (
                            <div className="overflow-y-auto">
                                <Accordion
                                    collapsible
                                    defaultValue={
                                        workflowExecution?.triggerExecution
                                            ?.id || ''
                                    }
                                    type="single"
                                >
                                    {workflowExecution?.triggerExecution && (
                                        <WorkflowTriggerAccordion
                                            triggerExecution={
                                                workflowExecution?.triggerExecution
                                            }
                                            triggerExecutionCompleted={
                                                triggerExecutionCompleted
                                            }
                                        />
                                    )}

                                    <WorkflowTaskListAccordion
                                        taskExecutions={
                                            workflowExecution.job.taskExecutions
                                        }
                                        taskExecutionsCompleted={
                                            !!taskExecutionsCompleted
                                        }
                                    />
                                </Accordion>
                            </div>
                        )}
                    </div>

                    {workflowExecution && (
                        <ReadOnlyWorkflow
                            execution={workflowExecution}
                            setWorkflowExecutionDetailsDialogOpen={
                                setWorkflowExecutionDetailsDialogOpen
                            }
                        />
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default WorkflowExecutionDetailsDialog;
