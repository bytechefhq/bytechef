import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import SubflowExecutionBreadcrumb, {
    BreadcrumbEntryI,
    TruncatedLabel,
} from '@/shared/components/workflow-executions/SubflowExecutionBreadcrumb';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {ExecutionError, Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowExecutionTaskExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {TabValueType} from '@/shared/types';
import {WorkflowIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';

interface WorkflowExecutionSheetContentProps {
    activeTab: TabValueType;
    deepestFailedExecution: {execution: TaskExecution | TriggerExecution; path: string[]} | null;
    dialogOpen: boolean;
    handleBreadcrumbNavigate: (index: number) => void;
    handleSeeExecutions: (childJob: Job) => void;
    handleTaskClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    isTriggerExecution: boolean;
    job: Job;
    jobFailedWithNoExecutions: boolean;
    jobFailureError: ExecutionError;
    rootJob?: Job;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    setActiveTab: (tab: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    subflowStack: Array<{job: Job; label: string}>;
    taskExecutions: TaskExecution[];
    triggerExecution?: TriggerExecution;
    workflowExecutionId: number;
}

const WorkflowExecutionSheetContent = ({
    activeTab,
    deepestFailedExecution,
    dialogOpen,
    handleBreadcrumbNavigate,
    handleSeeExecutions,
    handleTaskClick,
    isTriggerExecution,
    job,
    jobFailedWithNoExecutions,
    jobFailureError,
    rootJob,
    selectedItem,
    setActiveTab,
    setDialogOpen,
    subflowStack,
    taskExecutions,
    triggerExecution,
    workflowExecutionId,
}: WorkflowExecutionSheetContentProps) => {
    const isTaskSelected = !!selectedItem && !isTriggerExecution && selectedItem.id !== undefined;

    const {data: selectedTaskExecution, isLoading: selectedTaskExecutionLoading} =
        useGetWorkflowExecutionTaskExecutionQuery(
            {id: Number(workflowExecutionId), taskExecutionId: Number(selectedItem?.id)},
            isTaskSelected,
            false
        );

    const breadcrumbItems = useMemo<BreadcrumbEntryI[]>(() => {
        if (subflowStack.length === 0) {
            return [];
        }

        return [
            {label: rootJob?.label ?? 'Workflow', onNavigate: () => handleBreadcrumbNavigate(0)},
            ...subflowStack.slice(0, -1).map((entry, index) => ({
                label: entry.label,
                onNavigate: () => handleBreadcrumbNavigate(index + 1),
            })),
            {label: subflowStack[subflowStack.length - 1].label},
        ];
    }, [handleBreadcrumbNavigate, rootJob?.label, subflowStack]);

    const handleBreadcrumbBackClick = useCallback(() => {
        handleBreadcrumbNavigate(subflowStack.length - 1);
    }, [handleBreadcrumbNavigate, subflowStack.length]);

    return (
        <div className="flex size-full flex-col">
            <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />

            {jobFailedWithNoExecutions ? (
                <div className="flex-1 p-4">
                    <WorkflowExecutionContent error={jobFailureError} />
                </div>
            ) : (
                <ResizablePanelGroup orientation="horizontal">
                    <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={500}>
                        {subflowStack.length === 0 && rootJob && (
                            <div className="flex h-9 items-center gap-1 px-3 py-2">
                                <WorkflowIcon className="size-3 shrink-0 text-content-neutral-primary" />

                                <TruncatedLabel
                                    className="text-xs leading-4 font-medium text-content-neutral-primary"
                                    label={rootJob.label ?? ''}
                                />
                            </div>
                        )}

                        {subflowStack.length > 0 && (
                            <SubflowExecutionBreadcrumb
                                items={breadcrumbItems}
                                onBackClick={handleBreadcrumbBackClick}
                            />
                        )}

                        <ScrollArea className="min-h-0 flex-1 pr-4 pl-1">
                            <Accordion
                                className="ml-2 space-y-2"
                                defaultValue={
                                    deepestFailedExecution?.path ||
                                    (isTriggerExecution ? [triggerExecution?.id || ''] : [selectedItem?.id || ''])
                                }
                                type="multiple"
                            >
                                {triggerExecution && (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={deepestFailedExecution?.path}
                                        execution={triggerExecution}
                                        onExecutionClick={handleTaskClick}
                                        selectedExecutionId={selectedItem?.id || ''}
                                    >
                                        <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                                    </WorkflowExecutionsAccordionItem>
                                )}

                                {taskExecutions.map((taskExecution) => (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={deepestFailedExecution?.path}
                                        execution={taskExecution}
                                        key={taskExecution.id}
                                        onExecutionClick={handleTaskClick}
                                        selectedExecutionId={selectedItem?.id || ''}
                                    >
                                        <WorkflowTaskExecutionItem taskExecution={taskExecution} />
                                    </WorkflowExecutionsAccordionItem>
                                ))}
                            </Accordion>
                        </ScrollArea>
                    </ResizablePanel>

                    <ResizableHandle />

                    <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={500}>
                        <WorkflowExecutionsTabsPanel
                            activeTab={activeTab}
                            dialogOpen={dialogOpen}
                            job={job}
                            onSeeExecutionsClick={handleSeeExecutions}
                            selectedItem={selectedItem}
                            selectedItemDataLoading={isTaskSelected && selectedTaskExecutionLoading}
                            selectedItemInput={selectedTaskExecution?.input}
                            selectedItemOutput={selectedTaskExecution?.output}
                            setActiveTab={setActiveTab}
                            setDialogOpen={setDialogOpen}
                            triggerExecution={triggerExecution}
                        />
                    </ResizablePanel>
                </ResizablePanelGroup>
            )}
        </div>
    );
};

export default WorkflowExecutionSheetContent;
