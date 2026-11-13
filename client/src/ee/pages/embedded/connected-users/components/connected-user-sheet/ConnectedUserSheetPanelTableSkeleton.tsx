import {Skeleton} from '@/components/ui/skeleton';

interface ConnectedUserSheetPanelTableSkeletonProps {
    columnCount?: number;
    label: string;
    rowCount?: number;
}

const ConnectedUserSheetPanelTableSkeleton = ({
    columnCount = 5,
    label,
    rowCount = 5,
}: ConnectedUserSheetPanelTableSkeletonProps) => (
    <div aria-busy="true" aria-label={label} className="flex flex-col" role="status">
        <div className="flex items-center gap-x-6 border-b border-border/50 px-3 py-3">
            {Array.from({length: columnCount}, (_, columnIndex) => (
                <Skeleton className="h-4 flex-1" key={columnIndex} />
            ))}
        </div>

        {Array.from({length: rowCount}, (_, rowIndex) => (
            <div className="flex items-center gap-x-6 border-b border-border/50 px-3 py-4" key={rowIndex}>
                {Array.from({length: columnCount}, (_, columnIndex) => (
                    <Skeleton className="h-4 flex-1" key={columnIndex} />
                ))}
            </div>
        ))}
    </div>
);

export default ConnectedUserSheetPanelTableSkeleton;
