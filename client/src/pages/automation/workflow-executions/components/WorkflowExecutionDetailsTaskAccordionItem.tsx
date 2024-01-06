import WorkflowExecutionDetailsAccordionContent from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordionContent';
import {AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {CheckCircledIcon, CrossCircledIcon} from '@radix-ui/react-icons';
import {TaskExecutionModel} from 'middleware/helios/execution';
import InlineSVG from 'react-inlinesvg';

const WorkflowExecutionDetailsTaskAccordionItem = ({taskExecution}: {taskExecution: TaskExecutionModel}) => {
    const {component, endDate, error, id, input, output, startDate, workflowTask} = taskExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <AccordionItem key={id} value={id || ''}>
            <AccordionTrigger className="flex w-full items-center justify-between border-gray-100 bg-white px-2 py-3 data-[state=closed]:border-b">
                <div className="flex items-center space-x-1 text-sm">
                    {taskExecution?.component?.icon && (
                        <InlineSVG className="mr-1 h-6 w-6" src={taskExecution?.component?.icon} />
                    )}

                    <span>{component?.title}</span>

                    <span className="text-xs text-muted-foreground">({workflowTask?.name || workflowTask?.type})</span>
                </div>

                <div className="flex items-center">
                    <span className="ml-auto mr-2 text-xs">{duration}ms</span>

                    {taskExecution.status === 'COMPLETED' && <CheckCircledIcon className="h-5 w-5 text-green-500" />}

                    {taskExecution.status === 'FAILED' && <CrossCircledIcon className="h-5 w-5 text-red-500" />}
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

export default WorkflowExecutionDetailsTaskAccordionItem;
