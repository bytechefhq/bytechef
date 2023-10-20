import {CheckCircleIcon} from '@heroicons/react/24/outline';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {Separator} from '@radix-ui/react-select';
import Button from 'components/Button/Button';
import Tooltip from 'components/Tooltip/Tooltip';
import {useGetProjectExecutionQuery} from 'queries/projects.queries';
import {useEffect, useState} from 'react';

import useExecutionDetailsDialogStore from '../project/stores/useExecutionDetailsDialogStore';

const ExecutionDetailsDialog = ({selectedItemId}: {selectedItemId: number}) => {
    const {executionDetailsOpen, setExecutionDetailsOpen} =
        useExecutionDetailsDialogStore();

    const {data: currentExecution} = useGetProjectExecutionQuery(
        {
            id: selectedItemId,
        },
        executionDetailsOpen
    );

    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (currentExecution !== undefined) {
            setIsLoading(false);
        }
    }, [currentExecution]);

    const dummyData = {
        params: {},
        headers: {},
        body: {},
        method: 'POST',
        awaitingPayload: false,
    };

    return (
        <>
            {!isLoading && (
                <Dialog.Root
                    open={executionDetailsOpen}
                    onOpenChange={() =>
                        setExecutionDetailsOpen(!executionDetailsOpen)
                    }
                    modal={false}
                >
                    <Dialog.Portal>
                        <Dialog.Content className="fixed inset-y-0 right-0 z-10 flex w-full max-w-3xl overflow-hidden border-x-2 bg-gray-100 shadow-lg">
                            <div className="w-96 flex-col divide-y divide-gray-100 border-r-4 bg-gray-100 p-4 shadow-xl">
                                <Dialog.Title className="mb-5 flex items-center justify-between text-lg font-medium text-gray-900">
                                    {currentExecution?.taskExecutions &&
                                    currentExecution.taskExecutions.every(
                                        (taskExecution) =>
                                            taskExecution.status === 'COMPLETED'
                                    ) ? (
                                        <CheckCircleIcon className="mr-3 h-5 w-5 text-green-500" />
                                    ) : (
                                        <CheckCircleIcon className="mr-3 h-5 w-5 text-red-500" />
                                    )}

                                    {isLoading
                                        ? 'Workflow is loading. Please wait.'
                                        : currentExecution?.taskExecutions?.every(
                                              (taskExecution) =>
                                                  taskExecution.status ===
                                                  'COMPLETED'
                                          )
                                        ? 'Workflow executed successfully'
                                        : 'Workflow failed'}

                                    <Tooltip text="Information">
                                        <InfoCircledIcon className="mr-5 h-4 w-4" />
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
                                            setExecutionDetailsOpen(false)
                                        }
                                    />
                                </Dialog.Title>

                                <Accordion type="single">
                                    {currentExecution?.taskExecutions?.map(
                                        (taskExecution) => {
                                            return (
                                                <AccordionItem
                                                    key={taskExecution.id}
                                                    value={
                                                        taskExecution
                                                            .workflowTask
                                                            ?.label ||
                                                        'undefined'
                                                    }
                                                    className="mb-8"
                                                >
                                                    <AccordionTrigger className="w-full">
                                                        <span className="flex items-center justify-start md:justify-between">
                                                            <div className="flex items-center">
                                                                {currentExecution.taskExecutions &&
                                                                currentExecution.taskExecutions.every(
                                                                    (
                                                                        taskExecution
                                                                    ) =>
                                                                        taskExecution.status ===
                                                                        'COMPLETED'
                                                                ) ? (
                                                                    <CheckCircleIcon className="mr-3 h-5 w-5 text-green-500" />
                                                                ) : (
                                                                    <CheckCircleIcon className="mr-3 h-5 w-5 text-red-500" />
                                                                )}

                                                                {
                                                                    taskExecution
                                                                        .workflowTask
                                                                        ?.label
                                                                }
                                                            </div>

                                                            <span className="flex justify-start md:justify-between">
                                                                {taskExecution.lastModifiedDate?.getDate()}
                                                                s
                                                            </span>
                                                        </span>
                                                    </AccordionTrigger>

                                                    <Separator className="my-1 h-px bg-gray-300 dark:bg-gray-700" />

                                                    <AccordionContent>
                                                        <div className="rounded-lg border bg-white">
                                                            <span className="flex items-center justify-start bg-gray-200 md:justify-between">
                                                                <p className=" font-medium text-gray-900">
                                                                    Input
                                                                </p>

                                                                <span className="flex justify-start md:justify-between">
                                                                    {taskExecution.lastModifiedDate?.toLocaleString()}
                                                                </span>
                                                            </span>

                                                            <pre className="m-6 text-sm">
                                                                {JSON.stringify(
                                                                    taskExecution.input ||
                                                                        dummyData,
                                                                    null,
                                                                    2
                                                                )}
                                                            </pre>
                                                        </div>

                                                        <div className="mt-4 rounded-lg border bg-white">
                                                            <span className="flex items-center justify-start bg-gray-200 md:justify-between">
                                                                <p className=" font-medium text-gray-900">
                                                                    Output
                                                                </p>

                                                                <span className="flex justify-start md:justify-between">
                                                                    {taskExecution.lastModifiedDate?.toLocaleString()}
                                                                </span>
                                                            </span>

                                                            <pre className="m-6 text-sm">
                                                                {JSON.stringify(
                                                                    taskExecution.output ||
                                                                        dummyData,
                                                                    null,
                                                                    2
                                                                )}
                                                            </pre>
                                                        </div>
                                                    </AccordionContent>
                                                </AccordionItem>
                                            );
                                        }
                                    )}
                                </Accordion>
                            </div>

                            <div className="w-96 bg-white p-4">
                                <span className="text-lg font-medium">
                                    {currentExecution &&
                                    currentExecution.workflow?.label
                                        ? `${currentExecution.project?.name} / ${currentExecution.instance?.name} / ${currentExecution.workflow?.label}`
                                        : 'No data to show'}
                                </span>

                                <Button label="Edit" className="ml-4" />
                            </div>
                        </Dialog.Content>
                    </Dialog.Portal>
                </Dialog.Root>
            )}
        </>
    );
};

export default ExecutionDetailsDialog;
