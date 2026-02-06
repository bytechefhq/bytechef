import Button from '@/components/Button/Button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Spinner} from '@/components/ui/spinner';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon, WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';

const POLLING_INTERVAL_MS = 2000;

const WorkflowExecutionSheet = () => {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);

    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_4077 = useFeatureFlagsStore()('ff-4077');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

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

    const handleCopilotClick = useCallback(() => {
        const currentContext = useCopilotStore.getState().context;

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [setContext]);

    const handleCopilotClose = useCallback(() => {
        setCopilotPanelOpen(false);
    }, []);

    const handleOpenChange = useCallback(() => {
        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={workflowExecutionSheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>{`${workflowExecution?.project?.name}/${workflowExecution?.workflow?.label}`}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent className="absolute bottom-4 right-4 top-3 flex h-auto w-[90%] flex-row gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]">
                <div className="flex min-w-0 flex-1 flex-col">
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

                                <div className="flex items-center gap-1">
                                    {ff_4077 && copilotEnabled && (
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    className="[&_svg]:size-5"
                                                    icon={<SparklesIcon />}
                                                    onClick={handleCopilotClick}
                                                    size="icon"
                                                    variant="ghost"
                                                />
                                            </TooltipTrigger>

                                            <TooltipContent>Open Copilot panel</TooltipContent>
                                        </Tooltip>
                                    )}

                                    <SheetCloseButton />
                                </div>
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
                </div>

                <CopilotPanel
                    className={twMerge('h-full rounded-r-md border-l', !copilotPanelOpen && 'hidden')}
                    onClose={handleCopilotClose}
                />
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
