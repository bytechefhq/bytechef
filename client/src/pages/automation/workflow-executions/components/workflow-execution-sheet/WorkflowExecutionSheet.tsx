import Button from '@/components/Button/Button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Spinner} from '@/components/ui/spinner';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon, WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';

import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';
import useWorkflowExecutionSheet from './hooks/useWorkflowExecutionSheet';

const WorkflowExecutionSheet = () => {
    const {
        copilotEnabled,
        copilotPanelOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        workflowExecution,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    } = useWorkflowExecutionSheet();

    const ff_4077 = useFeatureFlagsStore()('ff-4077');

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
                                <ResizablePanelGroup className="h-full" orientation="horizontal">
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
                    className="h-full rounded-r-md border-l"
                    onClose={handleCopilotClose}
                    open={copilotPanelOpen}
                />
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
