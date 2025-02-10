import {Skeleton} from '@/components/ui/skeleton';

const ProjectSkeleton = () => {
    return (
        <header className="flex bg-background px-3 py-2.5">
            <div className="flex flex-1">
                <Skeleton className="h-9 w-1/5" />
            </div>

            <div className="flex items-center space-x-2">
                <Skeleton className="h-9 w-32" />

                <Skeleton className="h-9 w-24" />

                <Skeleton className="h-9 w-16" />

                <Skeleton className="h-9 w-16" />
            </div>
        </header>
    );
};

export default ProjectSkeleton;
