import {ExecutionError} from '@/shared/middleware/automation/workflow/execution';
import {Suspense, lazy, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {getFilteredOutput} from './WorkflowExecutionsUtils';

const ReactJson = lazy(() => import('react-json-view'));

interface WorkflowExecutionContentProps {
    dialogOpen?: boolean;
    error?: ExecutionError;
    input?: {[key: string]: string};
    output?: object;
    sheetOpen?: boolean;
    jobInputs?: {[key: string]: object};
    workflowTriggerName?: string;
}

const WorkflowExecutionContent = ({
    dialogOpen,
    error,
    input,
    jobInputs,
    output,
    sheetOpen,
    workflowTriggerName,
}: WorkflowExecutionContentProps) => {
    const filteredOutput = useMemo(
        () => getFilteredOutput(output, jobInputs, workflowTriggerName),
        [output, jobInputs, workflowTriggerName]
    );

    if (error !== undefined) {
        return (
            <div className="flex flex-col gap-4">
                <span className="mt-2 w-fit rounded-md border border-stroke-destructive-secondary p-2 text-sm font-semibold text-content-destructive-primary">
                    {error.message || 'No message.'}
                </span>

                {dialogOpen && (
                    <div className="flex flex-col space-y-1 pb-4">
                        <div className="text-sm font-semibold">Stack Trace</div>

                        {error?.stackTrace?.map((line, index) => (
                            <div className="text-sm" key={index}>
                                {line}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    if (input !== undefined) {
        return (
            <div className={twMerge('space-y-2 rounded-md p-2', sheetOpen && 'min-w-0 overflow-hidden')}>
                <div className="overflow-x-auto text-nowrap">
                    {input && (typeof input !== 'object' || Object.keys(input).length > 0) ? (
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

    if (output !== undefined || jobInputs !== undefined) {
        return (
            <>
                {filteredOutput ? (
                    <div className="space-y-2 rounded-md p-2">
                        <div className="overflow-x-auto text-nowrap">
                            {typeof filteredOutput === 'object' ? (
                                <Suspense
                                    fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}
                                >
                                    <ReactJson enableClipboard={false} src={filteredOutput as object} />
                                </Suspense>
                            ) : (
                                <span className="text-sm">{(filteredOutput as boolean).toString()}</span>
                            )}
                        </div>
                    </div>
                ) : (
                    <span className="text-sm">No output data.</span>
                )}
            </>
        );
    }

    return (
        // <>
        //     {input && (
        //         <div className={twMerge('space-y-2 rounded-md p-2', sheetOpen && 'min-w-0 overflow-hidden')}>
        //             <div className="overflow-x-auto text-nowrap">
        //                 {input && (typeof input !== 'object' || Object.keys(input).length > 0) ? (
        //                     typeof input === 'object' ? (
        //                         <Suspense
        //                             fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}
        //                         >
        //                             <ReactJson collapsed={false} enableClipboard={false} src={input as object} />
        //                         </Suspense>
        //                     ) : (
        //                         <span className="text-sm">{input}</span>
        //                     )
        //                 ) : (
        //                     <span className="text-sm">No input data.</span>
        //                 )}
        //             </div>
        //         </div>
        //     )}

        //     {error ? (
        //         <div className="flex flex-col gap-4">
        //             <span className="w-fit rounded-md border border-stroke-destructive-secondary p-2 text-sm font-semibold">
        //                 {error.message || 'No message.'}
        //             </span>

        //             {dialogOpen && (
        //                 <div className="flex flex-col space-y-1 pb-4">
        //                     <div className="text-sm font-semibold">Stack Trace</div>

        //                     {error?.stackTrace?.map((line, index) => (
        //                         <div className="text-sm" key={index}>
        //                             {line}
        //                         </div>
        //                     ))}
        //                 </div>
        //             )}
        //         </div>
        //     ) : filteredOutput ? (
        //         <div className="space-y-2 rounded-md p-2">
        //             <div className="overflow-x-auto text-nowrap">
        //                 {typeof filteredOutput === 'object' ? (
        //                     <Suspense fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}>
        //                         <ReactJson enableClipboard={false} src={filteredOutput as object} />
        //                     </Suspense>
        //                 ) : (
        //                     <span className="text-sm">{(filteredOutput as boolean).toString()}</span>
        //                 )}
        //             </div>
        //         </div>
        //     ) : (
        //         <span className="text-sm">No output data.</span>
        //     )}
        // </>
        null
    );
};

export default WorkflowExecutionContent;
