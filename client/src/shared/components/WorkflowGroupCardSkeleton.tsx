import {Skeleton} from '@/components/ui/skeleton';

const MAX_SKELETON_WORKFLOW_COUNT = 3;

interface WorkflowGroupCardSkeletonProps {
    label: string;
    showWorkflowRunButton?: boolean;
    workflowCount: number;
}

const WorkflowGroupCardSkeleton = ({label, showWorkflowRunButton, workflowCount}: WorkflowGroupCardSkeletonProps) => {
    const skeletonWorkflowCount = Math.min(workflowCount, MAX_SKELETON_WORKFLOW_COUNT);

    return (
        <div aria-busy="true" aria-label={label} className="rounded border border-border/50" role="status">
            <div className="flex w-full items-center justify-between px-3">
                <div className="flex-1 py-3">
                    <div className="flex items-center gap-2">
                        <Skeleton className="size-6 shrink-0" />

                        <Skeleton className="h-5 w-40" />
                    </div>

                    <Skeleton className="mt-2 h-4 w-20" />
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <Skeleton className="h-5 w-8 rounded-full" />

                    <div className="flex min-w-52 flex-col items-end gap-y-4">
                        <Skeleton className="h-5 w-9 rounded-full" />

                        <Skeleton className="h-3 w-40" />
                    </div>

                    <Skeleton className="size-9" />
                </div>
            </div>

            {skeletonWorkflowCount > 0 && (
                <div className="pt-3">
                    <Skeleton className="ml-3 h-4 w-20" />

                    <ul aria-label="Loading workflows">
                        {Array.from({length: skeletonWorkflowCount}, (_, index) => (
                            <li className="flex items-center justify-between px-3 py-1" key={index}>
                                <div className="flex min-w-0 flex-1 items-center">
                                    <div className="flex w-full max-w-80 items-center gap-2 sm:w-80">
                                        <Skeleton className="size-4 shrink-0" />

                                        <Skeleton className="h-4 w-28" />
                                    </div>

                                    <div className="ml-6 hidden items-center gap-1 sm:flex">
                                        <Skeleton className="size-7 rounded-full" />

                                        <Skeleton className="h-6 w-14 rounded-full" />

                                        <Skeleton className="size-7 rounded-full" />
                                    </div>
                                </div>

                                <div className="flex shrink-0 items-center gap-x-4">
                                    <Skeleton className="hidden h-3 w-40 xl:block" />

                                    <div className="flex items-center gap-x-6">
                                        {showWorkflowRunButton && (
                                            <Skeleton className="size-9" data-testid="workflow-run-button-skeleton" />
                                        )}

                                        <Skeleton className="mr-2 h-5 w-9 rounded-full" />
                                    </div>

                                    <Skeleton className="size-9" />
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default WorkflowGroupCardSkeleton;
