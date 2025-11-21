import {ExecutionError} from '@/shared/middleware/automation/workflow/execution';
import {Suspense, lazy, useMemo} from 'react';

import {getFilteredOutput, hasValue} from './WorkflowExecutionsUtils';

const ReactJson = lazy(() => import('react-json-view'));

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

    if (error !== undefined) {
        return (
            <div className="flex flex-col gap-4 overflow-hidden">
                <span className="w-fit rounded-md border border-stroke-destructive-secondary p-2 text-sm font-semibold text-content-destructive-primary">
                    {error.message || 'No message.'}
                </span>

                <div className="flex flex-col space-y-1">
                    <span className="text-sm font-semibold">Stack Trace</span>

                    {error?.stackTrace?.map((line, index) => (
                        <div className="text-sm" key={index}>
                            {line}
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (input !== undefined) {
        return (
            <div className="space-y-2 rounded-md">
                <div className="overflow-x-auto text-nowrap">
                    {hasValue(input) ? (
                        typeof input === 'object' ? (
                            <Suspense fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}>
                                <ReactJson collapsed={false} enableClipboard={false} src={input as object} />
                            </Suspense>
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
                        <Suspense fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}>
                            <ReactJson enableClipboard={false} src={filteredOutput as object} />
                        </Suspense>
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
