import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {Job, JobStatusEnum, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {TabValueType} from '@/shared/types';
import {useCallback, useMemo, useState} from 'react';

const getDeepestFailedExecution = (
    execution: TaskExecution | TriggerExecution,
    currentPath: string[] = []
): {execution: TaskExecution | TriggerExecution; path: string[]} | null => {
    const path = execution.id ? [...currentPath, execution.id] : currentPath;

    if ('iterations' in execution && execution.iterations && execution.iterations.length > 0) {
        for (let index = 0; index < execution.iterations.length; index++) {
            const iteration = execution.iterations[index];
            const iterationId = `${execution.id}-iteration-${index}`;

            for (const iterationTask of iteration) {
                const failedChild = getDeepestFailedExecution(iterationTask, [...path, iterationId]);

                if (failedChild) {
                    return failedChild;
                }
            }
        }
    }

    if ('children' in execution && execution.children && execution.children.length > 0) {
        for (const child of execution.children) {
            const failedChild = getDeepestFailedExecution(child, path);

            if (failedChild) {
                return failedChild;
            }
        }
    }

    if (execution.error) {
        return {execution, path};
    }

    return null;
};

const WorkflowExecutionSheetContent = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;
    const jobFailedWithNoExecutions = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;
    const jobFailureError = job.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const taskExecutions = useMemo(() => job?.taskExecutions || [], [job?.taskExecutions]);

    const deepestFailedExecutionResult = useMemo(() => {
        if (triggerExecution) {
            const result = getDeepestFailedExecution(triggerExecution);

            if (result) {
                return result;
            }
        }

        for (const taskExecution of taskExecutions) {
            const result = getDeepestFailedExecution(taskExecution);

            if (result) {
                return result;
            }
        }

        return null;
    }, [taskExecutions, triggerExecution]);

    const [activeTab, setActiveTab] = useState<TabValueType>(
        jobFailedWithNoExecutions || deepestFailedExecutionResult?.execution.error ? 'error' : 'output'
    );
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        deepestFailedExecutionResult?.execution || triggerExecution || job.taskExecutions?.[0] || undefined
    );

    const onTaskClick = useCallback((taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');
        setSelectedItem(taskExecution);
    }, []);

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

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
                        <ScrollArea className="mb-4 h-full pl-1 pr-4">
                            <Accordion
                                className="ml-2 space-y-2"
                                defaultValue={
                                    deepestFailedExecutionResult?.path ||
                                    (isTriggerExecution ? [triggerExecution?.id || ''] : [selectedItem?.id || ''])
                                }
                                type="multiple"
                            >
                                {triggerExecution && (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={deepestFailedExecutionResult?.path}
                                        execution={triggerExecution}
                                        onExecutionClick={onTaskClick}
                                        selectedExecutionId={selectedItem?.id || ''}
                                    >
                                        <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                                    </WorkflowExecutionsAccordionItem>
                                )}

                                {taskExecutions.map((taskExecution) => (
                                    <WorkflowExecutionsAccordionItem
                                        defaultValue={deepestFailedExecutionResult?.path}
                                        execution={taskExecution}
                                        key={taskExecution.id}
                                        onExecutionClick={onTaskClick}
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
                            selectedItem={selectedItem}
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
