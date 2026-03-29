import {Skeleton} from '@/components/ui/skeleton';

const IntegrationWorkflowsListSkeleton = () => {
    return (
        <ul className="flex flex-col items-center gap-4 pt-4">
            <li className="flex w-80 flex-col gap-1">
                <Skeleton className="mb-1 size-5 rounded-full" />

                <Skeleton className="h-6 w-full" />

                <Skeleton className="h-6 w-1/2" />
            </li>

            <li className="flex w-80 flex-col gap-1">
                <Skeleton className="mb-1 size-5 rounded-full" />

                <Skeleton className="h-6 w-full" />

                <Skeleton className="h-6 w-1/2" />
            </li>
        </ul>
    );
};

export default IntegrationWorkflowsListSkeleton;
