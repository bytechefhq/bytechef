import {Skeleton} from '@/components/ui/skeleton';

const MAX_SKELETON_WORKFLOW_COUNT = 3;

interface WorkflowListSkeletonProps {
    label: string;
    modifiedDateUnderName?: boolean;
    workflowCount?: number;
}

const WorkflowListSkeleton = ({label, modifiedDateUnderName = false, workflowCount = 1}: WorkflowListSkeletonProps) => {
    const skeletonWorkflowCount = Math.max(1, Math.min(workflowCount, MAX_SKELETON_WORKFLOW_COUNT));

    return (
        <ul aria-busy="true" aria-label={label} className="divide-y divide-stroke-neutral-primary">
            {Array.from({length: skeletonWorkflowCount}, (_, index) => (
                <li className="flex items-center justify-between py-1 pl-3" key={index}>
                    <div className="flex min-w-0 flex-1 items-center">
                        <div className="flex w-80 shrink-0 flex-col gap-1 pr-1">
                            <div className="flex h-5 items-center">
                                <Skeleton className="h-4 w-40" />
                            </div>

                            {modifiedDateUnderName && (
                                <div className="flex h-4 items-center xl:hidden" data-testid="modified-date-under-name">
                                    <Skeleton className="h-3 w-36" />
                                </div>
                            )}
                        </div>

                        <div className="hidden items-center gap-1 sm:flex">
                            <Skeleton className="size-7 rounded-full" />

                            <Skeleton className="h-6 w-24 rounded-full" />
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-3">
                        <Skeleton className={modifiedDateUnderName ? 'hidden h-3 w-40 xl:block' : 'h-3 w-40'} />

                        <Skeleton className="size-9" />
                    </div>
                </li>
            ))}
        </ul>
    );
};

export default WorkflowListSkeleton;
