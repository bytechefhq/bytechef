import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import {Skeleton} from '@/components/ui/skeleton';

const IntegrationBreadcrumbSkeleton = () => (
    <Breadcrumb aria-busy="true" aria-label="Loading integration">
        <BreadcrumbList>
            <BreadcrumbItem className="gap-2">
                <Skeleton className="h-5 w-40" />

                <Skeleton className="h-6 w-20 rounded-full" />
            </BreadcrumbItem>

            <BreadcrumbSeparator />

            <BreadcrumbItem className="px-1.5">
                <Skeleton className="h-5 w-44" />
            </BreadcrumbItem>
        </BreadcrumbList>
    </Breadcrumb>
);

export default IntegrationBreadcrumbSkeleton;
