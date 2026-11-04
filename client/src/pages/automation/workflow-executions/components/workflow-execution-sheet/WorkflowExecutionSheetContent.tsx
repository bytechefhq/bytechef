import Button from '@/components/Button/Button';
import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import {type LazyTaskExecutionChildrenOptionsI} from '@/shared/components/workflow-executions/LazyTaskExecutionChildren';
import SubflowExecutionBreadcrumb, {
    BreadcrumbEntryI,
} from '@/shared/components/workflow-executions/SubflowExecutionBreadcrumb';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {useWorkflowExecutionDetailQueries} from '@/shared/components/workflow-executions/providers/workflowExecutionDetailQueriesProvider';
import {getExecutionSelectionKey} from '@/shared/components/workflow-executions/util/executionSelectionKey';
import {TaskExecutionAttemptsI} from '@/shared/components/workflow-executions/util/groupTaskExecutionAttempts';
import {isClusterElementCallTaskExecution} from '@/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions';
import {isJobEnded} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {ExecutionError, Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {TabValueType} from '@/shared/types';
import {ReactNode, useCallback, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {type FocusedExecutionI} from './hooks/useWorkflowExecutionDetail';

interface WorkflowExecutionSheetContentProps {
    activeTab: TabValueType;
    dialogOpen: boolean;
    focusedExecution?: FocusedExecutionI;
    focusedTaskExecutionError?: boolean;
    handleBreadcrumbNavigate: (index: number) => void;
    handleFocusedTaskExecutionRetry?: () => void;
    handleLiveUpdatesRetry?: () => void;
    handleSeeExecutions: (childJob: Job) => void;
    handleTaskClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    headerActions?: ReactNode;
    isTriggerExecution: boolean;
    job?: Job;
    jobFailedWithNoExecutions: boolean;
    jobFailureError: ExecutionError;
    lazy: LazyTaskExecutionChildrenOptionsI;
    liveUpdatesError?: boolean;
    rootJob?: Job;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    setActiveTab: (tab: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    subflowStack: Array<{jobId: number; label: string}>;
    taskExecutionAttempts: TaskExecutionAttemptsI<TaskExecution>[];
    taskExecutionCount?: number;
    triggerExecution?: TriggerExecution;
}

const WorkflowExecutionSheetContent = ({
    activeTab,
    dialogOpen,
    focusedExecution,
    focusedTaskExecutionError,
    handleBreadcrumbNavigate,
    handleFocusedTaskExecutionRetry,
    handleLiveUpdatesRetry,
    handleSeeExecutions,
    handleTaskClick,
    headerActions,
    isTriggerExecution,
    job,
    jobFailedWithNoExecutions,
    jobFailureError,
    lazy,
    liveUpdatesError,
    rootJob,
    selectedItem,
    setActiveTab,
    setDialogOpen,
    subflowStack,
    taskExecutionAttempts,
    taskExecutionCount,
    triggerExecution,
}: WorkflowExecutionSheetContentProps) => {
    const isTaskSelected =
        !!selectedItem &&
        !isTriggerExecution &&
        selectedItem.id !== undefined &&
        !isClusterElementCallTaskExecution(selectedItem);

    const {useGetWorkflowExecutionTaskExecutionQuery} = useWorkflowExecutionDetailQueries();

    const {data: selectedTaskExecution, isLoading: selectedTaskExecutionLoading} =
        useGetWorkflowExecutionTaskExecutionQuery(
            {id: lazy.workflowExecutionId, taskExecutionId: Number(selectedItem?.id)},
            isTaskSelected,
            false
        );

    const selectedExecutionKey = getExecutionSelectionKey(selectedItem);

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
            <WorkflowExecutionsHeader
                actions={headerActions}
                job={job}
                taskExecutionCount={taskExecutionCount}
                triggerExecution={triggerExecution}
            />

            {jobFailedWithNoExecutions ? (
                <div className="flex-1 p-4">
                    <WorkflowExecutionContent error={jobFailureError} />
                </div>
            ) : (
                <ResizablePanelGroup orientation="horizontal">
                    <ResizablePanel
                        className="flex min-h-0 flex-col overflow-hidden"
                        defaultSize={34}
                        groupResizeBehavior="preserve-pixel-size"
                        minSize={15}
                    >
                        {subflowStack.length > 0 && (
                            <SubflowExecutionBreadcrumb
                                items={breadcrumbItems}
                                onBackClick={handleBreadcrumbBackClick}
                            />
                        )}

                        {liveUpdatesError && (
                            <div
                                className="flex items-center gap-x-2 px-3 py-2 text-xs text-content-neutral-secondary"
                                data-testid="live-updates-error"
                            >
                                <span>Live updates failed.</span>

                                {handleLiveUpdatesRetry && (
                                    <Button
                                        label="Retry"
                                        onClick={handleLiveUpdatesRetry}
                                        size="xs"
                                        variant="outline"
                                    />
                                )}
                            </div>
                        )}

                        {focusedTaskExecutionError && (
                            <div
                                className="flex items-center gap-x-2 px-3 py-2 text-xs text-content-neutral-secondary"
                                data-testid="focused-task-execution-error"
                            >
                                <span>Failed to load the focused task execution.</span>

                                {handleFocusedTaskExecutionRetry && (
                                    <Button
                                        label="Retry"
                                        onClick={handleFocusedTaskExecutionRetry}
                                        size="xs"
                                        variant="outline"
                                    />
                                )}
                            </div>
                        )}

                        <ScrollArea className="min-h-0 flex-1 pr-4 pl-1">
                            <Accordion
                                className={twMerge('ml-2 space-y-2 pb-4', subflowStack.length === 0 && 'pt-3')}
                                defaultValue={
                                    focusedExecution?.path || (isTriggerExecution ? [] : [selectedItem?.id || ''])
                                }
                                type="multiple"
                            >
                                {triggerExecution && (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={focusedExecution?.path}
                                        execution={triggerExecution}
                                        lazy={lazy}
                                        onExecutionClick={handleTaskClick}
                                        selectedExecutionKey={selectedExecutionKey}
                                    >
                                        <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                                    </WorkflowExecutionsAccordionItem>
                                )}

                                {taskExecutionAttempts.map(({latestTaskExecution, previousTaskExecutions}) => (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={focusedExecution?.path}
                                        execution={latestTaskExecution}
                                        jobEnded={isJobEnded(job)}
                                        key={latestTaskExecution.id}
                                        lazy={lazy}
                                        onExecutionClick={handleTaskClick}
                                        previousTaskExecutions={previousTaskExecutions}
                                        selectedExecutionKey={selectedExecutionKey}
                                    >
                                        <WorkflowTaskExecutionItem
                                            attemptLabel={
                                                previousTaskExecutions.length > 0
                                                    ? `Attempt ${previousTaskExecutions.length + 1}`
                                                    : undefined
                                            }
                                            jobEnded={isJobEnded(job)}
                                            taskExecution={latestTaskExecution}
                                        />
                                    </WorkflowExecutionsAccordionItem>
                                ))}
                            </Accordion>
                        </ScrollArea>
                    </ResizablePanel>

                    <ResizableHandle />

                    <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={66} minSize={20}>
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
