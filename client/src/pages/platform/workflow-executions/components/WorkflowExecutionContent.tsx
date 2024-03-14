import {Dialog, DialogClose, DialogContent, DialogHeader, DialogTitle, DialogTrigger} from '@/components/ui/dialog';
import {ExecutionErrorModel} from '@/middleware/automation/workflow/execution';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ExpandIcon} from 'lucide-react';
import ReactJson from 'react-json-view';

const WorkflowExecutionContent = ({
    endDate,
    error,
    input,
    output,
    startDate,
}: {
    endDate?: Date;
    error?: ExecutionErrorModel;
    input?: {[key: string]: string};
    output?: object;
    startDate?: Date;
}) => {
    return (
        <>
            <div className="space-y-2 rounded-md bg-gray-50 p-2">
                <header className="flex items-center justify-between">
                    <span className="text-sm font-semibold uppercase">Input</span>

                    <div className="flex items-center space-x-1">
                        <span className="text-xs">{startDate?.toLocaleString()}</span>

                        <Dialog>
                            <DialogTrigger asChild>
                                <ExpandIcon className="h-4 cursor-pointer" />
                            </DialogTrigger>

                            <DialogContent className="max-w-[1000px]">
                                <DialogHeader>
                                    <div className="flex items-center justify-between uppercase">
                                        <DialogTitle>Input</DialogTitle>

                                        <DialogClose asChild>
                                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                        </DialogClose>
                                    </div>
                                </DialogHeader>

                                <div className="max-h-[80vh] overflow-y-auto">
                                    {input && (typeof input !== 'object' || Object.keys(input).length > 0) ? (
                                        typeof input === 'object' ? (
                                            <ReactJson
                                                collapsed={false}
                                                enableClipboard={false}
                                                src={input as object}
                                            />
                                        ) : (
                                            input
                                        )
                                    ) : (
                                        <span className="text-sm">No input data.</span>
                                    )}
                                </div>
                            </DialogContent>
                        </Dialog>
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
                <div className="space-y-2 rounded-md bg-gray-50 p-2">
                    <header className="flex items-center justify-between">
                        <span className="text-sm font-semibold uppercase">Output</span>

                        <div className="flex items-center space-x-1">
                            <span className="text-xs">{endDate?.toLocaleString()}</span>

                            <Dialog>
                                <DialogTrigger asChild>
                                    <ExpandIcon className="h-4 cursor-pointer" />
                                </DialogTrigger>

                                <DialogContent className="max-w-[1000px]">
                                    <DialogHeader>
                                        <div className="flex items-center justify-between uppercase">
                                            <DialogTitle>Output</DialogTitle>

                                            <DialogClose asChild>
                                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                            </DialogClose>
                                        </div>
                                    </DialogHeader>

                                    <div className="max-h-[80vh] overflow-y-auto">
                                        {output ? (
                                            typeof output === 'object' ? (
                                                <ReactJson
                                                    collapsed={false}
                                                    enableClipboard={false}
                                                    src={output as object}
                                                />
                                            ) : (
                                                output
                                            )
                                        ) : (
                                            <span className="text-sm">No output data.</span>
                                        )}
                                    </div>
                                </DialogContent>
                            </Dialog>
                        </div>
                    </header>

                    <div className="overflow-x-auto text-nowrap">
                        {output ? (
                            typeof output === 'object' ? (
                                <ReactJson enableClipboard={false} src={output as object} />
                            ) : (
                                <span className="text-sm">{output}</span>
                            )
                        ) : (
                            <span className="text-sm">No output data.</span>
                        )}
                    </div>
                </div>
            )}

            {error && (
                <div className="space-y-2 rounded-md bg-gray-50 p-2">
                    <header className="flex items-center justify-between">
                        <span className="text-sm font-semibold uppercase text-destructive">Error</span>

                        <div className="flex space-x-1">
                            <span className="text-xs">{endDate?.toLocaleString()}</span>

                            <Dialog>
                                <DialogTrigger asChild>
                                    <ExpandIcon className="h-4 cursor-pointer" />
                                </DialogTrigger>

                                <DialogContent className="max-w-[1000px]">
                                    <DialogHeader>
                                        <div className="flex items-center justify-between uppercase">
                                            <DialogTitle className="text-destructive">Error</DialogTitle>

                                            <DialogClose asChild>
                                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                            </DialogClose>
                                        </div>
                                    </DialogHeader>

                                    <div className="max-h-[80vh] space-y-3 overflow-y-auto">
                                        <div className="flex flex-col space-y-1 text-sm">
                                            <div className="text-sm font-semibold">Message</div>

                                            <div className="text-sm">{error.message}</div>
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
                            <div className="text-sm">{error.message}</div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default WorkflowExecutionContent;
