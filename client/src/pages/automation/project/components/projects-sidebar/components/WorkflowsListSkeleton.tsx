import {Skeleton} from '@/components/ui/skeleton';

const WorkflowsListSkeleton = () => {
    return (
        <ul className="ml-2 mt-4 flex flex-col gap-7">
            <li className="flex flex-col gap-1">
                <Skeleton className="mb-1 size-5 rounded-full" />

                <Skeleton className="h-6 w-1/3" />

                <Skeleton className="h-6 w-2/3" />
            </li>

            <li className="flex flex-col gap-1">
                <Skeleton className="mb-1 size-5 rounded-full" />

                <Skeleton className="h-6 w-1/3" />

                <Skeleton className="h-6 w-2/3" />
            </li>
        </ul>
    );
};

export default WorkflowsListSkeleton;
