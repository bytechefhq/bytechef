import Button from '@/components/Button/Button';
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
import WorkflowExecutionLogsContent from '@/shared/components/workflow-executions/WorkflowExecutionLogsContent';
import {getDisplayValue, hasDialogContentValue} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {AlertCircleIcon, ExpandIcon, ScrollTextIcon} from 'lucide-react';
import {useMemo} from 'react';

type TabValueType = 'input' | 'output' | 'error' | 'logs';

const WorkflowExecutionsTabsPanel = ({
    activeTab,
    dialogOpen,
    isEditorEnvironment = false,
    job,
    selectedItem,
    setActiveTab,
    setDialogOpen,
    triggerExecution,
}: {
    activeTab: TabValueType;
    setActiveTab: (value: TabValueType) => void;
    dialogOpen: boolean;
    setDialogOpen: (open: boolean) => void;
    selectedItem: TriggerExecution | TaskExecution | undefined;
    job: Job;
    isEditorEnvironment?: boolean;
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

    const itemLabel = isTriggerExecution
        ? triggerExecution?.workflowTrigger?.label
        : selectedItem && 'workflowTask' in selectedItem
          ? (selectedItem as TaskExecution).workflowTask?.label
          : undefined;

    const workflowNodeName = isTriggerExecution
        ? triggerExecution?.workflowTrigger?.name
        : selectedItem && 'workflowTask' in selectedItem
          ? (selectedItem as TaskExecution).workflowTask?.name
          : undefined;

    return (
        <Tabs
            className="flex h-full flex-col"
            defaultValue={activeTab}
            onValueChange={(value) => setActiveTab(value as TabValueType)}
            value={activeTab}
        >
            <div className="flex items-center justify-between p-3">
                <div className="flex items-center gap-2">
                    <div>{selectedItem?.icon && <LazyLoadSVG className="size-5" src={selectedItem?.icon || ''} />}</div>

                    <span className="text-sm font-semibold">{itemLabel || selectedItem?.title}</span>

                    <span className="text-xs text-muted-foreground">{`(${workflowNodeName})`}</span>
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

            <div className="flex items-center justify-between p-3">
                <TabsList>
                    <TabsTrigger value="input">Input</TabsTrigger>

                    <TabsTrigger value="output">Output</TabsTrigger>

                    {selectedItem?.error && (
                        <TabsTrigger className="flex items-center gap-x-1" value="error">
                            <AlertCircleIcon className="size-4 text-content-destructive-primary" />

                            <span className="text-content-destructive-primary">Error</span>
                        </TabsTrigger>
                    )}

                    <TabsTrigger className="flex items-center gap-x-1" value="logs">
                        <ScrollTextIcon className="size-4" />

                        <span>Logs</span>
                    </TabsTrigger>
                </TabsList>

                {hasDialogContent && (
                    <div className="flex items-center gap-x-2">
                        <Dialog onOpenChange={setDialogOpen} open={dialogOpen}>
                            <DialogTrigger asChild>
                                <Button icon={<ExpandIcon />} size="iconXs" variant="ghost" />
                            </DialogTrigger>

                            <DialogContent className="max-w-workflow-execution-content-width">
                                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                                    <DialogTitle>{activeTab.toUpperCase()}</DialogTitle>

                                    <div className="flex items-center gap-2">
                                        <WorkflowExecutionContentClipboardButton value={displayValue} />

                                        <DialogCloseButton />
                                    </div>
                                </DialogHeader>

                                <ScrollArea className="max-h-workflow-execution-content-height overflow-auto pb-4 pr-4">
                                    <WorkflowExecutionContent
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

            <ScrollArea className="min-h-0 flex-1 pb-4 pr-4">
                <div className="mb-4">
                    <TabsContent className="p-3" value="input">
                        {selectedItem?.input ? (
                            <WorkflowExecutionContent input={selectedItem?.input} />
                        ) : (
                            <span className="p-1 text-sm">No input data.</span>
                        )}
                    </TabsContent>

                    <TabsContent className="p-3" value="output">
                        {selectedItem?.output !== undefined ? (
                            <WorkflowExecutionContent
                                jobInputs={isTriggerExecution ? job.inputs : undefined}
                                output={isTriggerExecution ? undefined : selectedItem?.output}
                                workflowTriggerName={
                                    isTriggerExecution ? triggerExecution?.workflowTrigger?.name : undefined
                                }
                            />
                        ) : (
                            <span className="p-1 text-sm">No output data.</span>
                        )}
                    </TabsContent>

                    <TabsContent className="p-3" value="error">
                        <WorkflowExecutionContent error={selectedItem?.error} />
                    </TabsContent>

                    <TabsContent className="h-full p-3" value="logs">
                        {job.id && (
                            <WorkflowExecutionLogsContent
                                isEditorEnvironment={isEditorEnvironment}
                                jobId={job.id}
                                taskExecutionId={
                                    selectedItem && 'workflowTask' in selectedItem
                                        ? (selectedItem as TaskExecution).id
                                        : undefined
                                }
                            />
                        )}
                    </TabsContent>
                </div>

                <ScrollBar orientation="horizontal" />

                <ScrollBar orientation="vertical" />
            </ScrollArea>
        </Tabs>
    );
};

export default WorkflowExecutionsTabsPanel;
