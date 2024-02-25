import WorkflowExecutionDetailsAccordionBadge from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordionBadge';
import WorkflowExecutionDetailsAccordionContent from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordionContent';
import {AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {TaskExecutionModel} from 'middleware/automation/workflow/execution';
import InlineSVG from 'react-inlinesvg';

const WorkflowExecutionDetailsTaskAccordionItem = ({taskExecution}: {taskExecution: TaskExecutionModel}) => {
    const {component, endDate, error, id, input, output, startDate, workflowTask} = taskExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <AccordionItem key={id} value={id || ''}>
            <AccordionTrigger className="flex w-full items-center justify-between border-gray-100 bg-white p-3 data-[state=closed]:border-b">
                <div className="flex items-center gap-x-2 text-sm">
                    <WorkflowExecutionDetailsAccordionBadge success={taskExecution.status === 'COMPLETED'} />

                    <div className="flex items-center gap-x-1">
                        {taskExecution?.component?.icon && (
                            <InlineSVG className="size-4" src={taskExecution?.component?.icon} />
                        )}

                        <span>{component?.title}</span>

                        <span className="text-xs text-muted-foreground">
                            ({workflowTask?.name || workflowTask?.type})
                        </span>
                    </div>
                </div>

                <div className="flex items-center">
                    <span className="ml-auto mr-2 text-xs">{duration}ms</span>
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
