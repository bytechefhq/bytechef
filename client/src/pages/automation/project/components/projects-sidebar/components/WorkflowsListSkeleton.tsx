import {Skeleton} from '@/components/ui/skeleton';

const WorkflowsListSkeleton = () => {
    return (
        <ul className="flex flex-col items-center gap-4 pt-4">
            <li className="flex w-80 flex-col gap-1">
                <div className="mb-3 flex items-center gap-2">
                    <Skeleton className="size-6 rounded-full" />

                    <Skeleton className="size-6 rounded-full" />
                </div>

                <Skeleton className="h-6 w-full" />

                <Skeleton className="h-6 w-1/2" />
            </li>

            <li className="flex w-80 flex-col gap-1">
                <div className="mb-3 flex items-center gap-2">
                    <Skeleton className="size-6 rounded-full" />

                    <Skeleton className="size-6 rounded-full" />
                </div>

                <Skeleton className="h-6 w-full" />

                <Skeleton className="h-6 w-1/2" />
            </li>
        </ul>
    );
};

export default WorkflowsListSkeleton;
