import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {ScrollArea, ScrollBar} from '@/components/ui/scroll-area';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionContentClipboardButton from '@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton';
import {getDisplayValue, hasDialogContentValue} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {AlertCircleIcon, ExpandIcon} from 'lucide-react';
import {useMemo} from 'react';

const WorkflowExecutionTabsContent = ({
    activeTab,
    dialogOpen,
    job,
    selectedItem,
    setActiveTab,
    setDialogOpen,
    triggerExecution,
}: {
    activeTab: 'input' | 'output' | 'error';
    setActiveTab: (value: 'input' | 'output' | 'error') => void;
    dialogOpen: boolean;
    setDialogOpen: (open: boolean) => void;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    job: Job;
    triggerExecution?: TriggerExecution;
}) => {
    const displayValue = useMemo(
        () =>
            getDisplayValue({
                job,
                selectedItem,
                tab: activeTab,
                triggerExecution,
            }),
        [job, selectedItem, activeTab, triggerExecution]
    );

    const hasDialogContent = useMemo(
        () =>
            hasDialogContentValue({
                job,
                selectedItem,
                tab: activeTab,
                triggerExecution,
            }),
        [job, selectedItem, activeTab, triggerExecution]
    );

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    return (
        <Tabs
            className="min-h-0 overflow-hidden"
            defaultValue={activeTab}
            onValueChange={(value) => setActiveTab(value as 'input' | 'output' | 'error')}
            value={activeTab}
        >
            <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <div>{selectedItem?.icon && <LazyLoadSVG className="size-5" src={selectedItem?.icon || ''} />}</div>

                    <span className="text-sm font-semibold">{selectedItem?.title}</span>

                    <span className="text-xs text-muted-foreground">
                        {`(${isTriggerExecution ? (selectedItem as TriggerExecution)?.workflowTrigger?.name : (selectedItem as TaskExecution)?.workflowTask?.name})`}
                    </span>
                </div>

                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <span>
                        {activeTab === 'input'
                            ? selectedItem?.startDate?.toLocaleDateString()
                            : selectedItem?.endDate?.toLocaleDateString()}
                    </span>

                    <span>
                        {activeTab === 'input'
                            ? selectedItem?.startDate?.toLocaleTimeString()
                            : selectedItem?.endDate?.toLocaleTimeString()}
                    </span>
                </div>
            </div>

            <div className="flex items-center justify-between">
                <TabsList>
                    <TabsTrigger value="input">Input</TabsTrigger>

                    <TabsTrigger value="output">Output</TabsTrigger>

                    {selectedItem?.error && (
                        <TabsTrigger className="flex items-center gap-x-1" value="error">
                            <AlertCircleIcon className="size-4 text-content-destructive-primary" />

                            <span className="text-content-destructive-primary">Error</span>
                        </TabsTrigger>
                    )}
                </TabsList>

                {hasDialogContent && (
                    <div className="ml-2 flex items-center gap-x-1">
                        <Dialog onOpenChange={setDialogOpen} open={dialogOpen}>
                            <DialogTrigger asChild>
                                <ExpandIcon className="h-4 cursor-pointer" />
                            </DialogTrigger>

                            <DialogContent className="max-w-workflow-execution-content-width">
                                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                                    <DialogTitle>{activeTab.toUpperCase()}</DialogTitle>

                                    <div className="flex items-center gap-1">
                                        <WorkflowExecutionContentClipboardButton value={displayValue} />

                                        <DialogCloseButton />
                                    </div>
                                </DialogHeader>

                                <ScrollArea className="max-h-workflow-execution-content-height overflow-auto pr-4">
                                    <WorkflowExecutionContent
                                        dialogOpen={dialogOpen}
                                        error={activeTab === 'error' ? selectedItem?.error : undefined}
                                        input={activeTab === 'input' ? selectedItem?.input : undefined}
                                        jobInputs={
                                            activeTab === 'output' && isTriggerExecution ? job.inputs : undefined
                                        }
                                        output={
                                            activeTab === 'output' && !isTriggerExecution
                                                ? selectedItem?.output
                                                : undefined
                                        }
                                        sheetOpen
                                        workflowTriggerName={
                                            activeTab === 'output' && isTriggerExecution
                                                ? triggerExecution?.workflowTrigger?.name
                                                : undefined
                                        }
                                    />

                                    <ScrollBar orientation="horizontal" />
                                </ScrollArea>
                            </DialogContent>
                        </Dialog>

                        <WorkflowExecutionContentClipboardButton value={displayValue} />
                    </div>
                )}
            </div>

            <ScrollArea className="h-full overflow-auto pr-4">
                <div>
                    <TabsContent value="input">
                        {selectedItem?.input ? (
                            <WorkflowExecutionContent input={selectedItem?.input} sheetOpen />
                        ) : (
                            <span className="p-1 text-sm">No input data.</span>
                        )}
                    </TabsContent>

                    <TabsContent value="output">
                        {selectedItem?.output ? (
                            <WorkflowExecutionContent
                                jobInputs={isTriggerExecution ? job.inputs : undefined}
                                output={isTriggerExecution ? undefined : selectedItem?.output}
                                sheetOpen
                                workflowTriggerName={
                                    isTriggerExecution ? triggerExecution?.workflowTrigger?.name : undefined
                                }
                            />
                        ) : (
                            <span className="p-1 text-sm">No output data.</span>
                        )}
                    </TabsContent>

                    <TabsContent value="error">
                        <WorkflowExecutionContent error={selectedItem?.error} sheetOpen />
                    </TabsContent>
                </div>

                <ScrollBar orientation="horizontal" />

                <ScrollBar orientation="vertical" />
            </ScrollArea>
        </Tabs>
    );
};

export default WorkflowExecutionTabsContent;
