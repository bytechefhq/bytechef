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
import React from 'react';
import ReactJson from 'react-json-view';

interface WorkflowExecutionContentProps {
    endDate?: Date;
    error?: ExecutionError;
    input?: {[key: string]: string};
    output?: object;
    startDate?: Date;
}

const WorkflowExecutionContent = ({endDate, error, input, output, startDate}: WorkflowExecutionContentProps) => (
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
                                        <ReactJson collapsed={false} enableClipboard={false} src={input as object} />
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
                        <ReactJson collapsed={false} enableClipboard={false} src={input as object} />
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
                                        <WorkflowExecutionContentClipboardButton value={output} />

                                        <DialogCloseButton />
                                    </div>
                                </DialogHeader>

                                <div className="max-h-workflow-execution-content-height overflow-y-auto">
                                    {output === undefined ? (
                                        <span className="text-sm">No output data.</span>
                                    ) : output && typeof output === 'object' ? (
                                        <ReactJson enableClipboard={false} src={output as object} />
                                    ) : (
                                        <span className="text-sm">{output}</span>
                                    )}
                                </div>
                            </DialogContent>
                        </Dialog>

                        <WorkflowExecutionContentClipboardButton value={output} />
                    </div>
                </header>

                <div className="overflow-x-auto text-nowrap">
                    {output === undefined && <span className="text-sm">No output data.</span>}

                    {output !== undefined &&
                        (typeof output === 'object' ? (
                            <ReactJson enableClipboard={false} src={output as object} />
                        ) : (
                            <span className="text-sm">{(output as boolean).toString()}</span>
                        ))}
                </div>
            </div>
        )}

        {error && (
            <div className="space-y-2 rounded-md bg-muted/50 p-2">
                <header className="flex items-center justify-between">
                    <span className="text-sm font-semibold uppercase text-destructive">Error</span>

                    <div className="flex space-x-1">
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
                                            {error?.stackTrace?.map((line, index) => <div key={index}>{line}</div>)}
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

export default WorkflowExecutionContent;
