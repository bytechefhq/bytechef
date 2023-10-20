import {CheckCircleIcon} from '@heroicons/react/24/outline';
import * as Dialog from '@radix-ui/react-dialog';
import {useGetWorkflowExecutionQuery} from 'queries/projects.queries';
import {twMerge} from 'tailwind-merge';

import useExecutionDetailsDialogStore from '../../project/stores/useExecutionDetailsDialogStore';
import ReadOnlyWorkflow from './dialog-content/ReadOnlyWorkflow';
import WorkflowTaskListAccordion from './dialog-content/WorkflowTaskListAccordion';

const ExecutionDetailsDialog = () => {
    const {
        currentExecutionId,
        executionDetailsDialogOpen,
        setExecutionDetailsDialogOpen,
    } = useExecutionDetailsDialogStore();

    const {data: currentExecution, isLoading: currentExecutionLoading} =
        useGetWorkflowExecutionQuery(
            {
                id: currentExecutionId,
            },
            executionDetailsDialogOpen
        );

    const allTasksCompleted = currentExecution?.taskExecutions?.every(
        (taskExecution) => taskExecution.status === 'COMPLETED'
    );

    return (
        <Dialog.Root
            open={executionDetailsDialogOpen}
            onOpenChange={() =>
                setExecutionDetailsDialogOpen(!executionDetailsDialogOpen)
            }
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content className="fixed inset-y-0 right-0 z-10 flex w-full max-w-6xl overflow-hidden border-l border-gray-100 shadow-lg">
                    {currentExecutionLoading && <span>Loading...</span>}

                    <div className="flex w-6/12 flex-col border-r border-gray-200 bg-white">
                        <Dialog.Title className="flex items-center justify-between px-2 py-4 text-gray-900">
                            <span className="text-lg">
                                {allTasksCompleted
                                    ? 'Workflow executed successfully'
                                    : 'Workflow failed'}
                            </span>

                            <CheckCircleIcon
                                className={twMerge(
                                    'mr-3 h-5 w-5',
                                    allTasksCompleted
                                        ? 'text-green-500'
                                        : 'text-red-500'
                                )}
                            />
                        </Dialog.Title>

                        {!!currentExecution?.taskExecutions?.length && (
                            <WorkflowTaskListAccordion
                                allTasksCompleted={!!allTasksCompleted}
                                taskExecutions={currentExecution.taskExecutions}
                            />
                        )}
                    </div>

                    {currentExecution && (
                        <ReadOnlyWorkflow
                            execution={currentExecution}
                            setExecutionDetailsDialogOpen={
                                setExecutionDetailsDialogOpen
                            }
                        />
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default ExecutionDetailsDialog;
