import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import WorkflowExecutionContentClipboardButton from '@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton';
import {ExecutionError} from '@/shared/middleware/automation/workflow/execution';
import {ExpandIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';

const ReactJson = lazy(() => import('react-json-view'));

interface WorkflowExecutionContentProps {
    endDate?: Date;
    error?: ExecutionError;
    input?: {[key: string]: string};
    output?: object;
    startDate?: Date;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    jobInputs?: {[key: string]: any};
    workflowTriggerName?: string;
}

const WorkflowExecutionContent = ({
    endDate,
    error,
    input,
    jobInputs,
    output,
    startDate,
    workflowTriggerName,
}: WorkflowExecutionContentProps) => {
    let filteredOutput = output;

    if (jobInputs && Object.keys(jobInputs).length) {
        filteredOutput = Object.keys(jobInputs)
            .filter((key) => key === workflowTriggerName)
            .map((key) => jobInputs[key]);
    }

    return (
        <>
            <div className="space-y-2 rounded-md p-2">
                <header className="flex items-center justify-between">
                    <span className="text-sm font-semibold uppercase">Input</span>

                    <div className="flex items-center space-x-1">
                        <span className="text-xs">{startDate?.toLocaleString()}</span>

                        <Dialog>
                            <DialogTrigger asChild>
                                <ExpandIcon className="h-4 cursor-pointer" />
                            </DialogTrigger>

                            <DialogContent className="max-w-workflow-execution-content-width">
                                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                                    <DialogTitle>Input</DialogTitle>

                                    <div className="flex items-center gap-1">
                                        <WorkflowExecutionContentClipboardButton value={input} />

                                        <DialogCloseButton />
                                    </div>
                                </DialogHeader>

                                <div className="max-h-workflow-execution-content-height overflow-y-auto">
                                    {input && (typeof input !== 'object' || Object.keys(input).length > 0) ? (
                                        typeof input === 'object' ? (
                                            <Suspense
                                                fallback={
                                                    <div className="p-4 text-sm text-muted-foreground">Loading...</div>
                                                }
                                            >
                                                <ReactJson
                                                    collapsed={false}
                                                    enableClipboard={false}
                                                    src={input as object}
                                                />
                                            </Suspense>
                                        ) : (
                                            input
                                        )
                                    ) : (
                                        <span className="text-sm">No input data.</span>
                                    )}
                                </div>
                            </DialogContent>
                        </Dialog>

                        <WorkflowExecutionContentClipboardButton value={input} />
                    </div>
                </header>

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

            {!error && (
                <div className="space-y-2 rounded-md p-2">
                    <header className="flex items-center justify-between">
                        <span className="text-sm font-semibold uppercase">Output</span>

                        <div className="flex items-center space-x-1">
                            <span className="text-xs">{endDate?.toLocaleString()}</span>

                            <Dialog>
                                <DialogTrigger asChild>
                                    <ExpandIcon className="h-4 cursor-pointer" />
                                </DialogTrigger>

                                <DialogContent className="max-w-workflow-execution-content-width">
                                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                                        <DialogTitle>Output</DialogTitle>

                                        <div className="flex items-center gap-1">
                                            <WorkflowExecutionContentClipboardButton value={filteredOutput} />

                                            <DialogCloseButton />
                                        </div>
                                    </DialogHeader>

                                    <div className="max-h-workflow-execution-content-height overflow-y-auto">
                                        {filteredOutput === undefined ? (
                                            <span className="text-sm">No output data.</span>
                                        ) : filteredOutput && typeof filteredOutput === 'object' ? (
                                            <Suspense
                                                fallback={
                                                    <div className="p-4 text-sm text-muted-foreground">Loading...</div>
                                                }
                                            >
                                                <ReactJson enableClipboard={false} src={filteredOutput as object} />
                                            </Suspense>
                                        ) : (
                                            <span className="text-sm">{filteredOutput}</span>
                                        )}
                                    </div>
                                </DialogContent>
                            </Dialog>

                            <WorkflowExecutionContentClipboardButton value={filteredOutput} />
                        </div>
                    </header>

                    <div className="overflow-x-auto text-nowrap">
                        {filteredOutput === undefined && <span className="text-sm">No output data.</span>}

                        {filteredOutput !== undefined &&
                            (typeof filteredOutput === 'object' ? (
                                <Suspense
                                    fallback={<div className="p-4 text-sm text-muted-foreground">Loading...</div>}
                                >
                                    <ReactJson enableClipboard={false} src={filteredOutput as object} />
                                </Suspense>
                            ) : (
                                <span className="text-sm">{(filteredOutput as boolean).toString()}</span>
                            ))}
                    </div>
                </div>
            )}

            {error && (
                <div className="space-y-2 rounded-md bg-muted/50 p-2">
                    <header className="flex items-center justify-between">
                        <span className="text-sm font-semibold uppercase text-destructive">Error</span>

                        <div className="flex items-center space-x-1">
                            <span className="text-xs">{endDate?.toLocaleString()}</span>

                            <Dialog>
                                <DialogTrigger asChild>
                                    <ExpandIcon className="h-4 cursor-pointer" />
                                </DialogTrigger>

                                <DialogContent className="max-w-workflow-execution-content-width">
                                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                                        <DialogTitle className="uppercase text-destructive">Error</DialogTitle>

                                        <DialogCloseButton />
                                    </DialogHeader>

                                    <div className="max-h-workflow-execution-content-height space-y-3 overflow-y-auto">
                                        <div className="flex flex-col space-y-1 text-sm">
                                            <div className="text-sm font-semibold">Message</div>

                                            <div className="text-sm">{error.message || 'No message.'}</div>
                                        </div>

                                        <div className="flex flex-col space-y-1">
                                            <div className="text-sm font-semibold">Stack Trace</div>

                                            <div className="text-sm">
                                                {error?.stackTrace?.map((line, index) => (
                                                    <div key={index}>{line}</div>
                                                ))}
                                            </div>
                                        </div>
                                    </div>
                                </DialogContent>
                            </Dialog>
                        </div>
                    </header>

                    <div className="overflow-x-auto">
                        <div className="flex flex-col space-y-1">
                            <span className="text-sm">{error.message || 'No message.'}</span>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default WorkflowExecutionContent;
