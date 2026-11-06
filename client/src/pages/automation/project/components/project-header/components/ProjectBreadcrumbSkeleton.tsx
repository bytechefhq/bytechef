import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import {Skeleton} from '@/components/ui/skeleton';

const ProjectBreadcrumbSkeleton = () => (
    <Breadcrumb aria-busy="true" aria-label="Loading project">
        <BreadcrumbList>
            <BreadcrumbItem className="gap-2">
                <Skeleton className="h-5 w-40" />

                <Skeleton className="h-5 w-20 rounded-full" />
            </BreadcrumbItem>

            <BreadcrumbSeparator />

            <BreadcrumbItem className="gap-1 px-1.5 py-1">
                <Skeleton className="h-5 w-40" />

                <Skeleton className="size-4" />
            </BreadcrumbItem>
        </BreadcrumbList>
    </Breadcrumb>
);

export default ProjectBreadcrumbSkeleton;
