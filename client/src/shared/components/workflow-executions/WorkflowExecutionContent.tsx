import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import JsonView from '@/shared/components/JsonView';
import {ExecutionError} from '@/shared/middleware/automation/workflow/execution';
import {ChevronRightIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {getFilteredOutput, hasValue} from './WorkflowExecutionsUtils';

interface WorkflowExecutionContentProps {
    error?: ExecutionError;
    input?: {[key: string]: string};
    output?: object;
    jobInputs?: {[key: string]: object};
    workflowTriggerName?: string;
}

const WorkflowExecutionContent = ({
    error,
    input,
    jobInputs,
    output,
    workflowTriggerName,
}: WorkflowExecutionContentProps) => {
    const filteredOutput = useMemo(
        () => getFilteredOutput(output, jobInputs, workflowTriggerName),
        [output, jobInputs, workflowTriggerName]
    );

    const [stackTraceOpen, setStackTraceOpen] = useState(false);

    if (error !== undefined) {
        return (
            <div className="flex flex-col gap-4 overflow-hidden">
                <span className="w-fit rounded-md border border-stroke-destructive-secondary p-2 text-sm font-semibold text-content-destructive-primary">
                    {error.message || 'No message.'}
                </span>

                {error.stackTrace && error.stackTrace.length > 0 && (
                    <Collapsible onOpenChange={setStackTraceOpen} open={stackTraceOpen}>
                        <CollapsibleTrigger className="flex cursor-pointer items-center gap-1">
                            <ChevronRightIcon
                                className={twMerge('size-4 transition-transform', stackTraceOpen && 'rotate-90')}
                            />

                            <span className="text-sm font-semibold">Stack Trace</span>
                        </CollapsibleTrigger>

                        <CollapsibleContent>
                            <div className="mt-2 flex flex-col space-y-1 pl-5 text-sm">
                                {error.stackTrace.map((line) => (
                                    <div className="text-sm" key={line}>
                                        {line}
                                    </div>
                                ))}
                            </div>
                        </CollapsibleContent>
                    </Collapsible>
                )}
            </div>
        );
    }

    if (input !== undefined) {
        return (
            <div className="space-y-2 rounded-md">
                <div className="overflow-x-auto text-nowrap">
                    {hasValue(input) ? (
                        typeof input === 'object' ? (
                            <JsonView src={input as object} />
                        ) : (
                            <span className="text-sm">{input}</span>
                        )
                    ) : (
                        <span className="text-sm">No input data.</span>
                    )}
                </div>
            </div>
        );
    }

    if (output !== undefined || (jobInputs !== undefined && workflowTriggerName)) {
        if (!hasValue(filteredOutput)) {
            return <span className="text-sm">No output data.</span>;
        }

        return (
            <div className="space-y-2 rounded-md">
                <div className="overflow-x-auto text-nowrap">
                    {typeof filteredOutput === 'object' && filteredOutput !== null ? (
                        <JsonView src={filteredOutput as object} />
                    ) : (
                        <span className="text-sm">{String(filteredOutput)}</span>
                    )}
                </div>
            </div>
        );
    }

    return null;
};

export default WorkflowExecutionContent;
