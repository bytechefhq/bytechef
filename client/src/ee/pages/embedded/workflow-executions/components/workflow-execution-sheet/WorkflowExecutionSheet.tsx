import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Skeleton} from '@/components/ui/skeleton';
import {useGetIntegrationWorkflowExecutionQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionDetail from './WorkflowExecutionDetail';

const WorkflowExecutionSheet = () => {
    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetIntegrationWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionSheetOpen
    );

    const integrationVersion = workflowExecution?.integrationInstanceConfiguration?.integrationVersion;

    const handleOpenChange = useCallback(() => {
        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={workflowExecutionSheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>{`${workflowExecution?.integration?.name ?? ''} / ${workflowExecution?.workflow?.label ?? ''}`}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex items-center gap-x-2">
                        <WorkflowIcon />

                        {workflowExecutionLoading ? (
                            <Skeleton className="h-6 w-48" />
                        ) : (
                            <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                                {`${workflowExecution?.integration?.name ?? ''} /`}

                                <strong className="text-content-neutral-primary">
                                    {workflowExecution?.workflow?.label}
                                </strong>

                                {integrationVersion != null && <span>{`/ V${integrationVersion}`}</span>}
                            </span>
                        )}
                    </div>

                    <SheetCloseButton />
                </header>

                <WorkflowExecutionDetail
                    enabled={workflowExecutionSheetOpen}
                    workflowExecutionId={workflowExecutionId}
                />
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowExecutionSheet;
