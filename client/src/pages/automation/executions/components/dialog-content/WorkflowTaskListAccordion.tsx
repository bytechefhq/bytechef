import {CheckCircleIcon} from '@heroicons/react/24/outline';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import {TaskExecutionModel} from 'middleware/project';
import {twMerge} from 'tailwind-merge';

const dummyOutputData = {
    params: {},
    headers: {},
    body: {},
    method: 'POST',
    awaitingPayload: false,
};

const WorkflowTaskListAccordion = ({
    allTasksCompleted,
    taskExecutions,
}: {
    allTasksCompleted: boolean;
    taskExecutions: TaskExecutionModel[];
}) => (
    <Accordion collapsible type="single">
        {taskExecutions.map((taskExecution) => {
            const {id, input, lastModifiedDate, output, workflowTask} =
                taskExecution;

            return (
                workflowTask?.label && (
                    <AccordionItem key={id} value={workflowTask?.label}>
                        <AccordionTrigger className="flex w-full items-center border-b bg-white p-2">
                            <div className="flex items-center">
                                <CheckCircleIcon
                                    className={twMerge(
                                        'mr-3 h-5 w-5',
                                        allTasksCompleted
                                            ? 'text-green-500'
                                            : 'text-red-500'
                                    )}
                                />

                                {workflowTask?.label}
                            </div>

                            <span className="ml-auto mr-2 text-sm">
                                {lastModifiedDate?.getDate()}s
                            </span>
                        </AccordionTrigger>

                        <AccordionContent className="space-y-2 border-b-2 p-2">
                            <div className="rounded-lg bg-white p-2">
                                <header className="flex items-center justify-between rounded-md bg-gray-200 px-2 py-1">
                                    <span className="text-sm font-medium uppercase">
                                        Input
                                    </span>

                                    <span className="text-sm">
                                        {lastModifiedDate?.toLocaleString()}
                                    </span>
                                </header>

                                <pre className="mt-2 text-sm">
                                    {JSON.stringify(input, null, 2)}
                                </pre>
                            </div>

                            <div className="rounded-lg bg-white p-2">
                                <header className="flex items-center justify-between rounded-md bg-gray-200 px-2 py-1">
                                    <span className="text-sm font-medium uppercase">
                                        Output
                                    </span>

                                    <span className="text-sm">
                                        {lastModifiedDate?.toLocaleString()}
                                    </span>
                                </header>

                                <pre className="mt-2 text-sm">
                                    {JSON.stringify(
                                        output || dummyOutputData,
                                        null,
                                        2
                                    )}
                                </pre>
                            </div>
                        </AccordionContent>
                    </AccordionItem>
                )
            );
        })}
    </Accordion>
);

export default WorkflowTaskListAccordion;
