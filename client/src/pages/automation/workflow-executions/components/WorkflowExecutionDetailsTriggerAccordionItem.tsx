import {TriggerExecutionModel} from '@/middleware/automation/workflow/execution';
import WorkflowExecutionDetailsAccordionContent from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordionContent';
import {AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {CheckCircledIcon, CrossCircledIcon} from '@radix-ui/react-icons';
import InlineSVG from 'react-inlinesvg';

const WorkflowExecutionDetailsTriggerAccordionItem = ({
    triggerExecution,
}: {
    triggerExecution: TriggerExecutionModel;
}) => {
    const {component, endDate, error, id, input, output, startDate, workflowTrigger} = triggerExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <AccordionItem key={id} value={id || ''}>
            <AccordionTrigger className="flex w-full items-center justify-between border-gray-100 bg-white px-2 py-3 data-[state=closed]:border-b">
                <div className="flex items-center text-sm">
                    {triggerExecution?.component?.icon && (
                        <InlineSVG className="mr-1 h-6 w-6" src={triggerExecution?.component?.icon} />
                    )}

                    <span>{component?.title}</span>

                    <span className="text-xs text-muted-foreground">
                        ({workflowTrigger?.name || workflowTrigger?.type})
                    </span>
                </div>

                <div className="flex items-center">
                    <span className="ml-auto mr-2 text-xs">{duration}ms</span>

                    {triggerExecution.status === 'COMPLETED' && <CheckCircledIcon className="h-5 w-5 text-green-500" />}

                    {triggerExecution.status === 'FAILED' && <CrossCircledIcon className="h-5 w-5 text-red-500" />}
                </div>
            </AccordionTrigger>

            <WorkflowExecutionDetailsAccordionContent
                endDate={endDate}
                error={error}
                input={input}
                output={output}
                startDate={startDate}
            />
        </AccordionItem>
    );
};

export default WorkflowExecutionDetailsTriggerAccordionItem;
