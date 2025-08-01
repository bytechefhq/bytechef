import {Skeleton} from '@/components/ui/skeleton';
import {twMerge} from 'tailwind-merge';

export const PanelSkeleton = () => (
    <div className="flex w-full max-w-workflow-node-details-panel-width flex-col gap-2 p-4">
        <Skeleton className="h-7 w-1/2" />

        <Skeleton className="h-5 w-3/4" />

        <Skeleton className="h-5 w-full" />

        <Skeleton className="h-5 w-1/2" />

        <Skeleton className="h-5 w-3/4" />

        <Skeleton className="h-5 w-full" />

        <Skeleton className="h-5 w-1/2" />
    </div>
);

export const RightSidebarSkeleton = () => (
    <div className="flex w-sidebar-width flex-col gap-4 p-4">
        <Skeleton className="h-8 w-3/4" />

        <Skeleton className="h-6 w-full" />

        <Skeleton className="h-6 w-1/2" />

        <Skeleton className="h-6 w-3/4" />
    </div>
);

export const SheetSkeleton = () => (
    <div className="w-workflow-outputs-sheet-dialog-width">
        <div className="flex items-center justify-between">
            <Skeleton className="h-6 w-1/2" />

            <Skeleton className="h-6 w-8" />
        </div>

        <div className="mt-4 space-y-2">
            <Skeleton className="h-4 w-full" />

            <Skeleton className="h-4 w-3/4" />
        </div>
    </div>
);

export const FieldsetSkeleton = ({bottomBorder = false, label}: {bottomBorder?: boolean; label: string}) => (
    <div className={twMerge('flex flex-col', bottomBorder && 'border-b border-muted p-4')}>
        <span className="text-sm font-medium leading-6">{label}</span>

        <Skeleton className="h-9 w-full" />
    </div>
);

export const PropertySkeleton = () => <Skeleton className="h-9 w-full" />;

export const DescriptionTabSkeleton = () => (
    <div className="flex flex-col gap-y-4 p-4">
        <div className="flex flex-col gap-y-2">
            <Skeleton className="h-6 w-1/4" />

            <Skeleton className="h-8 w-full" />
        </div>

        <div className="flex flex-col gap-y-2">
            <Skeleton className="h-6 w-1/4" />

            <Skeleton className="h-24 w-full" />
        </div>
    </div>
);

export const DataPillPanelSkeleton = () => (
    <ul className="flex flex-col">
        {Array.from({length: 4}).map((_, index) => (
            <li className="flex items-center space-x-4 border-b border-border/50 p-4" key={index}>
                <Skeleton className="size-6" />

                <Skeleton className="h-6 w-2/3" />

                <Skeleton className="h-6 w-1/5" />
            </li>
        ))}
    </ul>
);
