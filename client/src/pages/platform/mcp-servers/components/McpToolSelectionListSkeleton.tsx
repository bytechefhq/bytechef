import {Skeleton} from '@/components/ui/skeleton';

interface McpToolSelectionListSkeletonProps {
    showHeading?: boolean;
}

const McpToolSelectionListSkeleton = ({showHeading}: McpToolSelectionListSkeletonProps) => (
    <div aria-busy="true" aria-label="Loading tools" className="space-y-4" role="status">
        {showHeading && <Skeleton className="mt-6 h-5 w-12" data-testid="mcp-tools-heading-skeleton" />}

        <div className="flex items-center space-x-3">
            <Skeleton className="size-4 rounded-sm" />

            <Skeleton className="h-4 w-36" />
        </div>

        <div className="divide-y">
            {[1, 2, 3, 4].map((value) => (
                <div className="flex items-center space-x-3 py-3" key={value}>
                    <Skeleton className="size-4 rounded-sm" />

                    <div className="flex-1 space-y-1">
                        <Skeleton className="h-5 w-48" />

                        <Skeleton className="h-5 w-3/4" />
                    </div>
                </div>
            ))}
        </div>
    </div>
);

export default McpToolSelectionListSkeleton;
