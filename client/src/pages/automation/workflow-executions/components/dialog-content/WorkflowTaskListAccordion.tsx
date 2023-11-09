import {
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import {CheckCircledIcon} from '@radix-ui/react-icons';
import {TaskExecutionModel} from 'middleware/helios/execution';
import InlineSVG from 'react-inlinesvg';
import ReactJson from 'react-json-view';
import {twMerge} from 'tailwind-merge';

const WorkflowTaskListAccordion = ({
    taskExecutions,
    taskExecutionsCompleted,
}: {
    taskExecutions: TaskExecutionModel[];
    taskExecutionsCompleted: boolean;
}) => {
    return (
        <>
            {taskExecutions.map((taskExecution) => {
                const {endDate, id, input, output, startDate, workflowTask} =
                    taskExecution;

                return (
                    workflowTask?.label && (
                        <AccordionItem key={id} value={id || ''}>
                            <AccordionTrigger className="flex w-full items-center justify-between border-gray-100 bg-white px-2 py-3 data-[state=closed]:border-b">
                                <div className="flex items-center text-sm">
                                    {taskExecution?.componentDefinition
                                        ?.icon && (
                                        <InlineSVG
                                            className="mr-1 h-6 w-6"
                                            src={
                                                taskExecution
                                                    ?.componentDefinition?.icon
                                            }
                                        />
                                    )}

                                    {workflowTask?.name || workflowTask?.type}
                                </div>

                                <div className="flex items-center">
                                    {startDate && endDate && (
                                        <span className="ml-auto mr-2 text-xs">
                                            {Math.round(
                                                endDate?.getTime() -
                                                    startDate.getTime()
                                            )}
                                            ms
                                        </span>
                                    )}

                                    <CheckCircledIcon
                                        className={twMerge(
                                            'h-5 w-5',
                                            taskExecutionsCompleted
                                                ? 'text-green-500'
                                                : 'text-red-500'
                                        )}
                                    />
                                </div>
                            </AccordionTrigger>

                            <AccordionContent className="space-y-4 border-b border-gray-100 p-3">
                                <div className="rounded-lg">
                                    <header className="flex items-center justify-between rounded-md bg-gray-100 px-2 py-1">
                                        <span className="text-sm font-medium uppercase">
                                            Input
                                        </span>

                                        <span className="text-xs">
                                            {startDate?.toLocaleString()}
                                        </span>
                                    </header>

                                    {input && typeof input === 'object' ? (
                                        <ReactJson
                                            src={input as object}
                                            enableClipboard={false}
                                        />
                                    ) : (
                                        <pre className="mt-2 text-xs">
                                            {input}
                                        </pre>
                                    )}
                                </div>

                                <div className="rounded-lg">
                                    <header className="flex items-center justify-between rounded-md bg-gray-100 px-2 py-1">
                                        <span className="text-sm font-medium uppercase">
                                            Output
                                        </span>

                                        <span className="text-xs">
                                            {endDate?.toLocaleString()}
                                        </span>
                                    </header>

                                    <pre className="mt-2 text-sm">
                                        {output &&
                                        typeof output === 'object' ? (
                                            <ReactJson
                                                src={output as object}
                                                enableClipboard={false}
                                            />
                                        ) : (
                                            <pre className="mt-2 text-xs">
                                                {output || 'No output data.'}
                                            </pre>
                                        )}
                                    </pre>
                                </div>
                            </AccordionContent>
                        </AccordionItem>
                    )
                );
            })}
        </>
    );
};

export default WorkflowTaskListAccordion;
