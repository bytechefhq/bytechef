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
import {useCallback, useState} from 'react';

const WorkflowExecutionSheetContent = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const hasNoExecutions = !triggerExecution && (!job.taskExecutions || job.taskExecutions.length === 0);
    const jobFailedWithNoExecutions = hasNoExecutions && job.status === JobStatusEnum.Failed;

    const [activeTab, setActiveTab] = useState<TabValueType>(jobFailedWithNoExecutions ? 'error' : 'input');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        triggerExecution || job.taskExecutions?.[0] || undefined
    );

    const taskExecutions = job?.taskExecutions || [];

    const onTaskClick = useCallback((taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'input');
        setSelectedItem(taskExecution);
    }, []);

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    return (
        <div className="flex size-full flex-col">
            <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />

            {jobFailedWithNoExecutions ? (
                <div className="flex-1 p-4">
                    <WorkflowExecutionContent error={job.error} />
                </div>
            ) : (
                <ResizablePanelGroup className="px-2" orientation="horizontal">
                    <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={500}>
                        <ScrollArea className="mb-4 h-full pr-4">
                            <Accordion
                                className="ml-2 space-y-2"
                                defaultValue={
                                    isTriggerExecution ? [triggerExecution?.id || ''] : [selectedItem?.id || '']
                                }
                                type="multiple"
                            >
                                {triggerExecution && (
                                    <WorkflowExecutionsAccordionItem
                                        execution={triggerExecution}
                                        onExecutionClick={onTaskClick}
                                        selectedExecutionId={selectedItem?.id || ''}
                                    >
                                        <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                                    </WorkflowExecutionsAccordionItem>
                                )}

                                {taskExecutions.map((taskExecution) => (
                                    <WorkflowExecutionsAccordionItem
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
