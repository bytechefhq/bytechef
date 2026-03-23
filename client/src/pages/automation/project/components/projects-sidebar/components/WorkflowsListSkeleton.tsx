import {Skeleton} from '@/components/ui/skeleton';

const WorkflowsListSkeleton = () => {
    return (
        <ul className="ml-2 mt-4 flex flex-col gap-7">
            <li className="flex flex-col gap-1">
                <div className="mb-3 flex items-center gap-2">
                    <Skeleton className="size-6 rounded-full" />

                    <Skeleton className="size-6 rounded-full" />
                </div>

                <Skeleton className="h-6 w-11/12" />

                <Skeleton className="h-6 w-1/3" />
            </li>

            <li className="flex flex-col gap-1">
                <div className="mb-3 flex items-center gap-2">
                    <Skeleton className="size-6 rounded-full" />

                    <Skeleton className="size-6 rounded-full" />
                </div>

                <Skeleton className="h-6 w-11/12" />

                <Skeleton className="h-6 w-1/3" />
            </li>
        </ul>
    );
};

export default WorkflowsListSkeleton;
