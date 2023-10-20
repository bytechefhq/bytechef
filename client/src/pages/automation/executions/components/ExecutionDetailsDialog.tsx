import {CheckCircleIcon} from '@heroicons/react/24/outline';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import Tooltip from 'components/Tooltip/Tooltip';
import {useGetProjectExecutionQuery} from 'queries/projects.queries';
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
        useGetProjectExecutionQuery(
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
                <Dialog.Content className="fixed inset-y-0 right-0 z-10 flex w-full max-w-3xl overflow-hidden border-l bg-gray-100 shadow-lg">
                    {currentExecutionLoading && <span>Loading...</span>}

                    <div className="flex flex-col border-r">
                        <Dialog.Title className="flex items-center p-2 font-medium text-gray-900">
                            <CheckCircleIcon
                                className={twMerge(
                                    'mr-3 h-5 w-5',
                                    allTasksCompleted
                                        ? 'text-green-500'
                                        : 'text-red-500'
                                )}
                            />

                            <span>
                                {allTasksCompleted
                                    ? 'Workflow executed successfully'
                                    : 'Workflow failed'}
                            </span>

                            <Tooltip text="Information">
                                <InfoCircledIcon className="h-4 w-4" />
                            </Tooltip>

                            <Button
                                aria-label="Close panel"
                                className="ml-auto"
                                displayType="icon"
                                icon={
                                    <Cross1Icon
                                        className="h-3 w-3 cursor-pointer text-gray-900"
                                        aria-hidden="true"
                                    />
                                }
                                onClick={() =>
                                    setExecutionDetailsDialogOpen(false)
                                }
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
                        <ReadOnlyWorkflow execution={currentExecution} />
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default ExecutionDetailsDialog;
