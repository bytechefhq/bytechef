import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {ExecutionError, Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {TabValueType} from '@/shared/types';

interface WorkflowExecutionSheetContentProps {
    activeTab: TabValueType;
    deepestFailedExecution: {execution: TaskExecution | TriggerExecution; path: string[]} | null;
    dialogOpen: boolean;
    handleTaskClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    isTriggerExecution: boolean;
    job: Job;
    jobFailedWithNoExecutions: boolean;
    jobFailureError: ExecutionError;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    setActiveTab: (tab: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    taskExecutions: TaskExecution[];
    triggerExecution?: TriggerExecution;
}

const WorkflowExecutionSheetContent = ({
    activeTab,
    deepestFailedExecution,
    dialogOpen,
    handleTaskClick,
    isTriggerExecution,
    job,
    jobFailedWithNoExecutions,
    jobFailureError,
    selectedItem,
    setActiveTab,
    setDialogOpen,
    taskExecutions,
    triggerExecution,
}: WorkflowExecutionSheetContentProps) => (
    <div className="flex size-full flex-col">
        <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />

        {jobFailedWithNoExecutions ? (
            <div className="flex-1 p-4">
                <WorkflowExecutionContent error={jobFailureError} />
            </div>
        ) : (
            <ResizablePanelGroup orientation="horizontal">
                <ResizablePanel className="flex min-h-0 flex-col overflow-hidden" defaultSize={500}>
                    <ScrollArea className="mb-4 h-full pr-4 pl-1">
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

export default WorkflowExecutionSheetContent;
