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
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {ChevronDownIcon, RefreshCwIcon, RefreshCwOffIcon, WorkflowIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';

import useWorkflowExecutions from './properties/hooks/useWorkflowExecutions';

interface WorkflowExecutionsTestOutputProps {
    onCloseClick?: () => void;
    onEditSubflowClick?: (workflowUuid: string) => void;
    resizablePanelSize?: number;
    workflowIsRunning: boolean;
    workflowTestExecution?: WorkflowTestExecution;
}

const WorkflowExecutionsTestOutput = ({
    onCloseClick,
    onEditSubflowClick,
    resizablePanelSize = 300,
    workflowIsRunning,
    workflowTestExecution,
}: WorkflowExecutionsTestOutputProps) => {
    const {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleBreadcrumbNavigate,
        handleExecutionClick,
        handleSeeExecutions,
        isTriggerExecution,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        rootJob,
        selectedExecution,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutions,
        triggerExecution,
    } = useWorkflowExecutions({workflowTestExecution});

    const breadcrumbItems = useMemo<BreadcrumbEntryI[]>(() => {
        if (subflowStack.length === 0) {
            return [];
        }

        return [
            {label: rootJob?.label ?? 'Workflow', onNavigate: () => handleBreadcrumbNavigate(0)},
            ...subflowStack.slice(0, -1).map((entry, i) => ({
                label: entry.label,
                onNavigate: () => handleBreadcrumbNavigate(i + 1),
            })),
            {label: subflowStack[subflowStack.length - 1].label},
        ];
    }, [handleBreadcrumbNavigate, rootJob?.label, subflowStack]);

    const handleBreadcrumbBackClick = useCallback(() => {
        handleBreadcrumbNavigate(subflowStack.length - 1);
    }, [handleBreadcrumbNavigate, subflowStack.length]);

    return (
        <div className="flex size-full flex-col">
            <div className="flex items-center justify-between border-b border-stroke-neutral-secondary py-1">
                {job ? (
                    <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />
                ) : (
                    <span className="flex w-full items-center gap-x-3 px-3 py-4 text-sm uppercase">Test Output</span>
                )}

                {onCloseClick && (
                    <button className="p-2" onClick={onCloseClick}>
                        <ChevronDownIcon className="h-5" />
                    </button>
                )}
            </div>

            <div className="relative size-full">
                <div className="absolute inset-0 overflow-y-auto">
                    {workflowIsRunning && (
                        <div className="flex size-full items-center justify-center gap-x-1 p-3">
                            <span className="flex animate-spin text-gray-400">
                                <RefreshCwIcon className="size-5" />
                            </span>

                            <span className="text-muted-foreground">Workflow is running...</span>
                        </div>
                    )}

                    {!workflowIsRunning && (
                        <>
                            {workflowTestExecution?.job && jobFailedWithNoExecutions && (
                                <div className="flex-1 p-4">
                                    <WorkflowExecutionContent error={jobFailureError} />
                                </div>
                            )}

                            {workflowTestExecution?.job && !jobFailedWithNoExecutions && (
                                <ResizablePanelGroup orientation="horizontal">
                                    <ResizablePanel
                                        className="flex flex-col overflow-hidden py-4"
                                        defaultSize={resizablePanelSize}
                                    >
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
                                                    (isTriggerExecution
                                                        ? [triggerExecution?.id || '']
                                                        : [selectedExecution?.id || ''])
                                                }
                                                type="multiple"
                                            >
                                                {triggerExecution && (
                                                    <WorkflowExecutionsAccordionItem
                                                        defaultValue={deepestFailedExecution?.path}
                                                        execution={triggerExecution}
                                                        onExecutionClick={handleExecutionClick}
                                                        selectedExecutionId={selectedExecution?.id || ''}
                                                    >
                                                        <WorkflowTriggerExecutionItem
                                                            triggerExecution={triggerExecution}
                                                        />
                                                    </WorkflowExecutionsAccordionItem>
                                                )}

                                                {taskExecutions.map((taskExecution) => (
                                                    <WorkflowExecutionsAccordionItem
                                                        defaultValue={deepestFailedExecution?.path}
                                                        execution={taskExecution}
                                                        key={taskExecution.id}
                                                        onExecutionClick={handleExecutionClick}
                                                        selectedExecutionId={selectedExecution?.id || ''}
                                                    >
                                                        <WorkflowTaskExecutionItem taskExecution={taskExecution} />
                                                    </WorkflowExecutionsAccordionItem>
                                                ))}
                                            </Accordion>
                                        </ScrollArea>
                                    </ResizablePanel>

                                    <ResizableHandle className="bg-muted" />

                                    <ResizablePanel className="flex min-h-0 flex-col space-y-4 overflow-hidden p-4">
                                        {job && (
                                            <WorkflowExecutionsTabsPanel
                                                activeTab={activeTab}
                                                dialogOpen={dialogOpen}
                                                isEditorEnvironment
                                                job={job}
                                                onEditSubflowClick={onEditSubflowClick}
                                                onSeeExecutionsClick={handleSeeExecutions}
                                                selectedItem={selectedExecution}
                                                setActiveTab={setActiveTab}
                                                setDialogOpen={setDialogOpen}
                                                triggerExecution={triggerExecution}
                                            />
                                        )}
                                    </ResizablePanel>
                                </ResizablePanelGroup>
                            )}

                            {!workflowTestExecution?.job && (
                                <div className="flex size-full items-center justify-center gap-x-1 p-3 text-muted-foreground">
                                    <RefreshCwOffIcon className="size-5" />

                                    <span>The workflow has not yet been executed.</span>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default WorkflowExecutionsTestOutput;
