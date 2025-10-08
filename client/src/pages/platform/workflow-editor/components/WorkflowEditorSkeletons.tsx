import LoadingDots from '@/components/LoadingDots';
import {Skeleton} from '@/components/ui/skeleton';
import {twMerge} from 'tailwind-merge';

export const WorkflowNodeDetailsPanelSkeleton = () => (
    <div
        className={twMerge(
            'absolute bottom-6 right-[69px] top-2 z-10 flex w-screen max-w-workflow-node-details-panel-width flex-col gap-2 overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background p-4'
        )}
    >
        <Skeleton className="h-8 w-3/4" />

        <Skeleton className="mt-2 h-12 w-full" />

        <div className="my-4 flex space-x-4">
            {Array.from({length: 4}).map((_, index) => (
                <Skeleton className="h-6 w-1/4" key={index} />
            ))}
        </div>

        <div className="flex flex-col gap-8">
            {Array.from({length: 4}).map((_, index) => (
                <PropertySkeleton key={index} />
            ))}
        </div>
    </div>
);

export const WorkflowRightSidebarSkeleton = () => (
    <div className="absolute right-0 m-2 flex flex-col items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-background p-1">
        {Array.from({length: 4}).map((_, index) => (
            <Skeleton className="size-9" key={index} />
        ))}
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
    <div className="absolute bottom-6 right-data-pill-panel-placement top-2 z-10 w-screen max-w-data-pill-panel-width overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background">
        <ul className="flex flex-col">
            {Array.from({length: 12}).map((_, index) => (
                <li className="flex items-center space-x-4 border-b border-border/50 p-4" key={index}>
                    <Skeleton className="size-6" />

                    <Skeleton className="h-6 w-2/3" />

                    <Skeleton className="h-6 w-1/5" />
                </li>
            ))}
        </ul>
    </div>
);

export const PropertiesTabSkeleton = () => (
    <div className="flex flex-col gap-4 p-4">
        {Array.from({length: 4}).map((_, index) => (
            <PropertySkeleton key={index} />
        ))}
    </div>
);

export const WorkflowCodeEditorSheetSkeleton = () => (
    <div className="flex size-full flex-col">
        <header className="flex w-full items-center justify-between border-b border-b-border/50 p-3">
            <h2 className="text-lg font-semibold text-foreground">Edit Workflow</h2>

            <div className="flex items-center gap-1">
                {Array.from({length: 4}).map((_, index) => (
                    <Skeleton className="size-9" key={index} />
                ))}
            </div>
        </header>

        <div className="flex size-full flex-col items-center justify-center p-4">
            <LoadingDots />
        </div>

        <div className="h-2/5 w-full" />
    </div>
);

export const WorkflowSheetSkeleton = ({title}: {title: string}) => (
    <div className="flex size-full flex-col">
        <header className="flex w-full items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">{title}</h2>

            <Skeleton className="size-9" />
        </header>

        <div className="flex size-full flex-col items-center justify-center p-4">
            <LoadingDots />
        </div>
    </div>
);

export const WorkflowNodesSidebarSkeleton = () => (
    <aside className="absolute inset-y-2 right-14 flex w-96 flex-col gap-2 overflow-hidden rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary p-4">
        <Skeleton className="h-9 w-full" />

        <div className="flex space-x-2">
            {Array.from({length: 3}).map((_, index) => (
                <Skeleton className="h-7 w-1/3" key={index} />
            ))}
        </div>

        <div className="flex justify-between">
            <Skeleton className="h-9 w-1/6" />

            <Skeleton className="size-9" />
        </div>

        <div className="flex flex-col space-y-2">
            {Array.from({length: 12}).map((_, index) => (
                <Skeleton className="h-12 w-full" key={index} />
            ))}
        </div>
    </aside>
);
