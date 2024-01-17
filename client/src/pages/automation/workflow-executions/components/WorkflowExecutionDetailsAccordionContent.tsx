import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogHeader, DialogTitle, DialogTrigger} from '@/components/ui/dialog';
import {ExecutionErrorModel} from '@/middleware/automation/workflow/execution';
import {AccordionContent} from '@radix-ui/react-accordion';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ExpandIcon} from 'lucide-react';
import ReactJson from 'react-json-view';

const WorkflowExecutionDetailsAccordionContent = ({
    endDate,
    error,
    input,
    output,
    startDate,
}: {
    endDate: Date | undefined;
    error?: ExecutionErrorModel;
    input?: {[key: string]: string};
    output?: object;
    startDate: Date | undefined;
}) => {
    return (
        <AccordionContent className="space-y-4 border-b border-gray-100 p-3">
            <div className="space-y-2 rounded-lg">
                <header className="flex items-center justify-between rounded-md bg-gray-100 px-2 py-1">
                    <span className="text-sm font-medium uppercase">Input</span>

                    <div className="flex items-center space-x-1">
                        <span className="text-xs">{startDate?.toLocaleString()}</span>

                        <Dialog>
                            <DialogTrigger asChild>
                                <Button size="icon" variant="ghost">
                                    <ExpandIcon className="h-4" />
                                </Button>
                            </DialogTrigger>

                            <DialogContent className="max-w-[1000px]">
                                <DialogHeader>
                                    <div className="flex items-center justify-between">
                                        <DialogTitle>Input</DialogTitle>

                                        <DialogClose asChild>
                                            <Button size="icon" variant="ghost">
                                                <Cross2Icon className="size-4 opacity-70" />
                                            </Button>
                                        </DialogClose>
                                    </div>
                                </DialogHeader>

                                <div className="max-h-[80vh] overflow-y-auto">
                                    {input ? (
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

                <div className="overflow-x-auto">
                    {input ? (
                        typeof input === 'object' ? (
                            <ReactJson collapsed={false} enableClipboard={false} src={input as object} />
                        ) : (
                            input
                        )
                    ) : (
                        <span className="text-sm">No input data.</span>
                    )}
                </div>
            </div>

            <div className="space-y-2 rounded-lg">
                <header className="flex items-center justify-between rounded-md bg-gray-100 px-2 py-1">
                    <span className="text-sm font-medium uppercase">Output</span>

                    <div className="flex items-center space-x-1">
                        <span className="text-xs">{endDate?.toLocaleString()}</span>

                        <Dialog>
                            <DialogTrigger asChild>
                                <Button size="icon" variant="ghost">
                                    <ExpandIcon className="h-4" />
                                </Button>
                            </DialogTrigger>

                            <DialogContent className="max-w-[1000px]">
                                <DialogHeader>
                                    <div className="flex items-center justify-between">
                                        <DialogTitle>Output</DialogTitle>

                                        <DialogClose asChild>
                                            <Button size="icon" variant="ghost">
                                                <Cross2Icon className="size-4 opacity-70" />
                                            </Button>
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

                <div className="overflow-x-auto">
                    {output ? (
                        typeof output === 'object' ? (
                            <ReactJson enableClipboard={false} src={output as object} />
                        ) : (
                            output
                        )
                    ) : (
                        <span className="text-sm">No output data.</span>
                    )}
                </div>
            </div>

            {error && (
                <div className="space-y-2 rounded-lg">
                    <header className="flex items-center justify-between rounded-md bg-gray-100 px-2 py-1">
                        <span className="text-sm font-medium uppercase text-destructive">Error</span>

                        <div className="flex space-x-1">
                            <span className="text-xs">{endDate?.toLocaleString()}</span>

                            <Dialog>
                                <DialogTrigger asChild>
                                    <Button size="icon" variant="ghost">
                                        <ExpandIcon className="h-4" />
                                    </Button>
                                </DialogTrigger>

                                <DialogContent className="max-w-[1000px]">
                                    <DialogHeader>
                                        <div className="flex items-center justify-between">
                                            <DialogTitle className="text-destructive">Error</DialogTitle>

                                            <DialogClose asChild>
                                                <Button size="icon" variant="ghost">
                                                    <Cross2Icon className="size-4 opacity-70" />
                                                </Button>
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
                            <div className="text-sm font-semibold">Message</div>

                            <div className="text-sm">{error.message}</div>
                        </div>
                    </div>
                </div>
            )}
        </AccordionContent>
    );
};

export default WorkflowExecutionDetailsAccordionContent;
