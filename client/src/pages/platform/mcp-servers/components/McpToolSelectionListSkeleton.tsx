import {Skeleton} from '@/components/ui/skeleton';

const McpToolSelectionListSkeleton = () => (
    <div aria-busy="true" aria-label="Loading tools" className="space-y-4" role="status">
        <div className="flex items-center space-x-3">
            <Skeleton className="size-4 rounded-sm" />

            <Skeleton className="h-4 w-36" />
        </div>

        <div className="divide-y">
            {[1, 2, 3, 4].map((value) => (
                <div className="flex items-start space-x-3 py-3" key={value}>
                    <Skeleton className="mt-0.5 size-4 rounded-sm" />

                    <div className="flex-1 space-y-2">
                        <Skeleton className="h-4 w-48" />

                        <Skeleton className="h-3 w-3/4" />
                    </div>
                </div>
            ))}
        </div>
    </div>
);

export default McpToolSelectionListSkeleton;
