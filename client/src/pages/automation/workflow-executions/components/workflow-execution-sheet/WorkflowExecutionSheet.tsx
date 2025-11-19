import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import {Spinner} from '@/components/ui/spinner';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
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

    const handleOpenChange = useCallback(() => {
        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={workflowExecutionSheetOpen}>
            <SheetContent className="flex h-full w-[90%] gap-0 p-0 sm:max-w-[90%]">
                {workflowExecutionLoading ? (
                    <div className="flex size-full items-center justify-center">
                        <Spinner className="size-6" />
                    </div>
                ) : (
                    <ResizablePanelGroup direction="horizontal">
                        <ResizablePanel
                            className="flex h-full w-1/2 flex-col border-r border-stroke-neutral-secondary bg-surface-neutral-primary"
                            defaultSize={50}
                        >
                            {workflowExecution?.job && (
                                <WorkflowExecutionSheetContent
                                    job={workflowExecution.job}
                                    triggerExecution={workflowExecution?.triggerExecution}
                                />
                            )}
                        </ResizablePanel>

                        <ResizableHandle />

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
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
