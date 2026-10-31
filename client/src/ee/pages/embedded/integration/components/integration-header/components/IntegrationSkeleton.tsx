import {Skeleton} from '@/components/ui/skeleton';
import IntegrationBreadcrumbSkeleton from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumbSkeleton';

const IntegrationSkeleton = () => {
    return (
        <header className="flex items-center justify-between bg-surface-main px-3 py-2.5">
            <div className="flex items-center gap-2">
                <Skeleton className="size-6" />

                <IntegrationBreadcrumbSkeleton />
            </div>

            <div className="flex items-center gap-4">
                <Skeleton className="size-6" />

                <Skeleton className="size-4 rounded-full" />

                <Skeleton className="size-6" />

                <div className="flex gap-2">
                    <Skeleton className="h-9 w-28" />

                    <Skeleton className="h-9 w-20" />
                </div>
            </div>
        </header>
    );
};

export default IntegrationSkeleton;
