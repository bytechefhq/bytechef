import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon, WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';

import WorkflowExecutionDetail from './WorkflowExecutionDetail';
import useWorkflowExecutionSheet from './hooks/useWorkflowExecutionSheet';

const WorkflowExecutionSheet = () => {
    const {
        copilotEnabled,
        copilotPanelOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        workflowExecution,
        workflowExecutionId,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    } = useWorkflowExecutionSheet();

    const ff_4077 = useFeatureFlagsStore()('ff-4077');

    return (
        <Sheet onOpenChange={handleOpenChange} open={workflowExecutionSheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>{`${workflowExecution?.project?.name}/${workflowExecution?.workflow?.label}`}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-row gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <div className="flex min-w-0 flex-1 flex-col">
                    <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                        <div className="flex items-center gap-x-2">
                            <WorkflowIcon />

                            {workflowExecutionLoading ? (
                                <Skeleton className="h-6 w-48" />
                            ) : (
                                <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                                    {`${workflowExecution?.project?.name} /`}

                                    <strong className="text-content-neutral-primary">
                                        {workflowExecution?.workflow?.label}
                                    </strong>
                                </span>
                            )}
                        </div>

                        <div className="flex items-center gap-1">
                            {ff_4077 && copilotEnabled && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            className="[&_svg]:size-5"
                                            disabled={workflowExecutionLoading}
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

                    <WorkflowExecutionDetail
                        enabled={workflowExecutionSheetOpen}
                        workflowExecutionId={workflowExecutionId}
                    />
                </div>

                <CopilotPanel
                    className="h-full rounded-r-md border-l border-l-border/50"
                    onClose={handleCopilotClose}
                    open={copilotPanelOpen}
                />
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
