import {TriggerExecutionModel} from '@/middleware/embedded/workflow/execution';
import {AccordionContent, AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {CheckCircledIcon} from '@radix-ui/react-icons';
import InlineSVG from 'react-inlinesvg';
import ReactJson from 'react-json-view';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionTriggerAccordionItem = ({
    triggerExecution,
    triggerExecutionCompleted,
}: {
    triggerExecution: TriggerExecutionModel;
    triggerExecutionCompleted: boolean;
}) => {
    const {endDate, id, input, output, startDate, workflowTrigger} = triggerExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <>
            {workflowTrigger?.label && (
                <AccordionItem key={id} value={id || ''}>
                    <AccordionTrigger className="flex w-full items-center justify-between border-gray-100 bg-white p-3 data-[state=closed]:border-b">
                        <div className="flex items-center text-sm">
                            {triggerExecution?.component?.icon && (
                                <InlineSVG className="mr-1 size-6" src={triggerExecution?.component?.icon} />
                            )}

                            {workflowTrigger?.name || workflowTrigger?.type}
                        </div>

                        <div className="flex items-center">
                            <span className="ml-auto mr-2 text-xs">{duration}ms</span>

                            <CheckCircledIcon
                                className={twMerge(
                                    'h-5 w-5',
                                    triggerExecutionCompleted ? 'text-green-500' : 'text-red-500'
                                )}
                            />
                        </div>
                    </AccordionTrigger>

                    <AccordionContent className="space-y-4 border-b border-gray-100 p-3">
                        <div className="rounded-lg">
                            <header className="flex items-center justify-between rounded-md bg-gray-100 px-2">
                                <span className="text-sm font-medium uppercase">Input</span>

                                <span className="text-xs">{startDate?.toLocaleString()}</span>
                            </header>

                            {input && typeof input === 'object' ? (
                                <ReactJson enableClipboard={false} src={input as object} />
                            ) : (
                                <pre className="mt-2 text-xs">{input}</pre>
                            )}
                        </div>

                        <div className="rounded-lg">
                            <header className="flex items-center justify-between rounded-md bg-gray-100 px-2">
                                <span className="text-sm font-medium uppercase">Output</span>

                                <span className="text-xs">{endDate?.toLocaleString()}</span>
                            </header>

                            <pre className="mt-2 text-sm">
                                {output && typeof output === 'object' ? (
                                    <ReactJson enableClipboard={false} src={output as object} />
                                ) : (
                                    <pre className="mt-2 text-xs">{output || 'No output data.'}</pre>
                                )}
                            </pre>
                        </div>
                    </AccordionContent>
                </AccordionItem>
            )}
        </>
    );
};

export default WorkflowExecutionTriggerAccordionItem;
