import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetAccordion from './WorkflowExecutionSheetAccordion';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionSheetOpen
    );

    return (
        <Sheet
            onOpenChange={() => setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen)}
            open={workflowExecutionSheetOpen}
        >
            <SheetContent className="flex h-full w-[90%] gap-0 p-0 sm:max-w-[90%]">
                {workflowExecutionLoading && <span>Loading...</span>}

                <ResizablePanelGroup direction="horizontal">
                    <ResizablePanel
                        className="flex h-full w-1/2 flex-col border-r border-r-border/50 bg-white"
                        defaultSize={50}
                    >
                        {workflowExecution?.job && (
                            <WorkflowExecutionSheetAccordion
                                job={workflowExecution.job}
                                triggerExecution={workflowExecution?.triggerExecution}
                            />
                        )}
                    </ResizablePanel>

                    <ResizableHandle className="bg-muted" />

                    <ResizablePanel className="w-1/2" defaultSize={50}>
                        {workflowExecution && (
                            <WorkflowReadOnlyProvider
                                value={{
                                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                }}
                            >
                                <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />
                            </WorkflowReadOnlyProvider>
                        )}
                    </ResizablePanel>
                </ResizablePanelGroup>
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
