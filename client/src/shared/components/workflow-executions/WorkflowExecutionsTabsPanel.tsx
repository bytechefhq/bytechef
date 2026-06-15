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
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {TabValueType} from '@/shared/types';
import {AlertCircleIcon, ExpandIcon, ScrollTextIcon, SquarePenIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';

interface WorkflowExecutionsTabsPanelProps {
    activeTab: TabValueType;
    dialogOpen: boolean;
    isEditorEnvironment?: boolean;
    job: Job;
    onEditSubflowClick?: (workflowUuid: string) => void;
    onSeeExecutionsClick?: (job: Job) => void;
    selectedItem: TriggerExecution | TaskExecution | undefined;
    selectedItemDataLoading?: boolean;
    selectedItemInput?: TaskExecution['input'];
    selectedItemOutput?: TaskExecution['output'];
    setActiveTab: (value: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    triggerExecution?: TriggerExecution;
}

const WorkflowExecutionsTabsPanel = ({
    activeTab,
    dialogOpen,
    isEditorEnvironment = false,
    job,
    onEditSubflowClick,
    onSeeExecutionsClick,
    selectedItem,
    selectedItemDataLoading,
    selectedItemInput,
    selectedItemOutput,
    setActiveTab,
    setDialogOpen,
    triggerExecution,
}: WorkflowExecutionsTabsPanelProps) => {
    const ff_2896 = useFeatureFlagsStore()('ff-2896');

    const resolvedInput = selectedItemInput !== undefined ? selectedItemInput : selectedItem?.input;
    const resolvedOutput = selectedItemOutput !== undefined ? selectedItemOutput : selectedItem?.output;

    const handleValueChange = useCallback((value: string) => setActiveTab(value as TabValueType), [setActiveTab]);

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

    const subflowTaskExecution =
        !isTriggerExecution && selectedItem && 'workflowTask' in selectedItem
            ? (selectedItem as TaskExecution)
            : undefined;

    const subflowWorkflowUuid = subflowTaskExecution?.workflowTask?.parameters?.workflowUuid as string | undefined;

    const childJob = subflowTaskExecution?.childJob;

    const handleSeeExecutionsClick = useCallback(() => {
        onSeeExecutionsClick?.(childJob!);
    }, [onSeeExecutionsClick, childJob]);

    const handleEditSubflowClick = useCallback(() => {
        onEditSubflowClick?.(subflowWorkflowUuid!);
    }, [onEditSubflowClick, subflowWorkflowUuid]);

    return (
        <Tabs
            className="flex h-full flex-col"
            defaultValue={activeTab}
            onValueChange={handleValueChange}
            value={activeTab}
        >
            <div className="flex items-center justify-between p-3">
                <div className="flex items-center gap-2">
                    <div>{selectedItem?.icon && <LazyLoadSVG className="size-5" src={selectedItem?.icon || ''} />}</div>

                    <span className="text-sm font-semibold">{itemLabel || selectedItem?.title}</span>

                    <span className="text-xs text-muted-foreground">{`(${workflowNodeName})`}</span>

                    {childJob && onSeeExecutionsClick && (
                        <Button
                            className="ml-2"
                            label="See Executions"
                            onClick={handleSeeExecutionsClick}
                            size="xxs"
                            variant="default"
                        />
                    )}

                    {subflowWorkflowUuid && onEditSubflowClick && (
                        <Button
                            icon={<SquarePenIcon />}
                            label="Edit"
                            onClick={handleEditSubflowClick}
                            size="xxs"
                            variant="outline"
                        />
                    )}
                </div>

                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <span>
                        {activeTab === 'input'
                            ? selectedItem?.startDate &&
                              (selectedItem.startDate instanceof Date
                                  ? selectedItem.startDate.toLocaleDateString()
                                  : new Date(selectedItem.startDate).toLocaleDateString())
                            : selectedItem?.endDate &&
                              (selectedItem.endDate instanceof Date
                                  ? selectedItem.endDate.toLocaleDateString()
                                  : new Date(selectedItem.endDate).toLocaleDateString())}
                    </span>

                    <span>
                        {activeTab === 'input'
                            ? selectedItem?.startDate &&
                              (selectedItem.startDate instanceof Date
                                  ? selectedItem.startDate.toLocaleTimeString()
                                  : new Date(selectedItem.startDate).toLocaleTimeString())
                            : selectedItem?.endDate &&
                              (selectedItem.endDate instanceof Date
                                  ? selectedItem.endDate.toLocaleTimeString()
                                  : new Date(selectedItem.endDate).toLocaleTimeString())}
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

                    {ff_2896 && (
                        <TabsTrigger className="flex items-center gap-x-1" value="logs">
                            <ScrollTextIcon className="size-4" />

                            <span>Logs</span>
                        </TabsTrigger>
                    )}
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

                                <ScrollArea className="max-h-workflow-execution-content-height overflow-auto pr-4 pb-4">
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

            <ScrollArea className="min-h-0 flex-1 pr-4 pb-4">
                <div className="mb-4">
                    <TabsContent className="p-3" value="input">
                        {selectedItemDataLoading ? (
                            <div className="flex items-center justify-center p-4">
                                <span className="text-sm text-muted-foreground">Loading…</span>
                            </div>
                        ) : resolvedInput ? (
                            <WorkflowExecutionContent input={resolvedInput} />
                        ) : (
                            <div className="flex items-center justify-center p-4">
                                <span className="text-sm text-muted-foreground">No input data</span>
                            </div>
                        )}
                    </TabsContent>

                    <TabsContent className="p-3" value="output">
                        {selectedItemDataLoading ? (
                            <div className="flex items-center justify-center p-4">
                                <span className="text-sm text-muted-foreground">Loading…</span>
                            </div>
                        ) : resolvedOutput !== undefined ? (
                            <WorkflowExecutionContent
                                jobInputs={isTriggerExecution ? job.inputs : undefined}
                                output={isTriggerExecution ? undefined : resolvedOutput}
                                workflowTriggerName={
                                    isTriggerExecution ? triggerExecution?.workflowTrigger?.name : undefined
                                }
                            />
                        ) : (
                            <div className="flex items-center justify-center p-4">
                                <span className="text-sm text-muted-foreground">No output data</span>
                            </div>
                        )}
                    </TabsContent>

                    <TabsContent className="p-3" value="error">
                        <WorkflowExecutionContent error={selectedItem?.error} />
                    </TabsContent>

                    {ff_2896 && (
                        <TabsContent className="h-full p-3" value="logs">
                            {job.id && (
                                <WorkflowExecutionLogsContent
                                    isEditorEnvironment={isEditorEnvironment}
                                    jobId={job.id}
                                    taskExecutionId={
                                        selectedItem && 'workflowTask' in selectedItem ? selectedItem.id : undefined
                                    }
                                />
                            )}
                        </TabsContent>
                    )}
                </div>

                <ScrollBar orientation="horizontal" />

                <ScrollBar orientation="vertical" />
            </ScrollArea>
        </Tabs>
    );
};

export default WorkflowExecutionsTabsPanel;
