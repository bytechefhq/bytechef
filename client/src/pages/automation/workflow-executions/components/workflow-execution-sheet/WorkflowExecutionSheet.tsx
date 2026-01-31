import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Spinner} from '@/components/ui/spinner';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';

const POLLING_INTERVAL_MS = 2000;

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

    const isWorkflowRunning = useMemo(() => {
        if (!workflowExecution?.job) {
            return false;
        }

        return getWorkflowStatusType(workflowExecution.job, workflowExecution.triggerExecution) === 'running';
    }, [workflowExecution]);

    useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId},
        workflowExecutionSheetOpen && isWorkflowRunning,
        POLLING_INTERVAL_MS
    );

    const handleOpenChange = useCallback(() => {
        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={workflowExecutionSheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>{`${workflowExecution?.project?.name}/${workflowExecution?.workflow?.label}`}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent className="absolute bottom-4 right-4 top-3 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]">
                {workflowExecutionLoading ? (
                    <div className="flex size-full items-center justify-center">
                        <Spinner className="size-6" />
                    </div>
                ) : (
                    <>
                        <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md bg-surface-neutral-primary p-3">
                            <div className="flex items-center gap-x-2">
                                <WorkflowIcon />

                                <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                                    {`${workflowExecution?.project?.name} /`}

                                    <strong className="text-content-neutral-primary">
                                        {workflowExecution?.workflow?.label}
                                    </strong>
                                </span>
                            </div>

                            <SheetCloseButton />
                        </header>

                        <div className="flex min-h-0 flex-1 p-3">
                            <ResizablePanelGroup className="h-full" direction="horizontal">
                                <ResizablePanel
                                    className="flex min-h-0 w-1/2 flex-col overflow-hidden rounded-md bg-surface-neutral-primary"
                                    defaultSize={50}
                                >
                                    {workflowExecution?.job && (
                                        <WorkflowExecutionSheetContent
                                            job={workflowExecution.job}
                                            triggerExecution={workflowExecution?.triggerExecution}
                                        />
                                    )}
                                </ResizablePanel>

                                <ResizableHandle className="mx-2.5" withHandle />

                                <ResizablePanel
                                    className="flex min-h-0 w-1/2 flex-col overflow-hidden"
                                    defaultSize={50}
                                >
                                    {workflowExecution && (
                                        <WorkflowReadOnlyProvider
                                            value={{
                                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                            }}
                                        >
                                            <WorkflowExecutionSheetWorkflowPanel
                                                workflowExecution={workflowExecution}
                                            />
                                        </WorkflowReadOnlyProvider>
                                    )}
                                </ResizablePanel>
                            </ResizablePanelGroup>
                        </div>
                    </>
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
