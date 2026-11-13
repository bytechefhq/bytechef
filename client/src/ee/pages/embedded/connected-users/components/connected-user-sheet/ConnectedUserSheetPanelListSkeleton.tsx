import {Skeleton} from '@/components/ui/skeleton';

interface ConnectedUserSheetPanelListSkeletonProps {
    label: string;
    rowCount?: number;
}

const ConnectedUserSheetPanelListSkeleton = ({label, rowCount = 3}: ConnectedUserSheetPanelListSkeletonProps) => (
    <ul aria-busy="true" aria-label={label} role="status">
        {Array.from({length: rowCount}, (_, rowIndex) => (
            <li
                className="mb-2 flex items-center justify-between rounded border border-border/50 px-3 py-3"
                key={rowIndex}
            >
                <div className="flex flex-1 items-center gap-x-3">
                    <Skeleton className="size-6 shrink-0 rounded-full" />

                    <div className="flex flex-col gap-y-2">
                        <Skeleton className="h-5 w-48" />

                        <Skeleton className="h-4 w-32" />
                    </div>
                </div>

                <div className="flex items-center gap-x-4">
                    <Skeleton className="h-5 w-20 rounded-full" />

                    <Skeleton className="h-5 w-9 rounded-full" />
                </div>
            </li>
        ))}
    </ul>
);

export default ConnectedUserSheetPanelListSkeleton;
