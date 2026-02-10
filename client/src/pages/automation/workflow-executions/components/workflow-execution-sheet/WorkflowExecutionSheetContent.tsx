import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowExecutionsTaskAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsTaskAccordionItem';
import WorkflowExecutionsTriggerAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsTriggerAccordionItem';
import {getTasksTree} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {TabValueType} from '@/shared/types';
import {useCallback, useMemo, useState} from 'react';

const WorkflowExecutionSheetContent = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('input');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        triggerExecution || job.taskExecutions?.[0] || undefined
    );

    const tasksTree = useMemo(() => (job ? getTasksTree(job) : []), [job]);

    const onTaskClick = useCallback((taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'input');
        setSelectedItem(taskExecution);
    }, []);

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    return (
        <div className="flex size-full flex-col">
            <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />

            <ResizablePanelGroup orientation="horizontal">
                <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={500}>
                    <ScrollArea className="mb-4 h-full pl-1 pr-4">
                        <Accordion
                            className="space-y-2"
                            collapsible
                            defaultValue={isTriggerExecution ? triggerExecution?.id || '' : selectedItem?.id || ''}
                            type="single"
                        >
                            {triggerExecution && (
                                <WorkflowExecutionsTriggerAccordionItem
                                    onTaskClick={onTaskClick}
                                    selectedItem={selectedItem}
                                    triggerExecution={triggerExecution}
                                />
                            )}

                            {tasksTree.map((taskTreeItem) => (
                                <WorkflowExecutionsTaskAccordionItem
                                    key={taskTreeItem.task.id}
                                    onTaskClick={onTaskClick}
                                    selectedTaskExecutionId={selectedItem?.id || ''}
                                    taskTreeItem={taskTreeItem}
                                />
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
        </div>
    );
};

export default WorkflowExecutionSheetContent;
