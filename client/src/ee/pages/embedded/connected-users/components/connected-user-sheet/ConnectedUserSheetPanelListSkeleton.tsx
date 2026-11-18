import {Skeleton} from '@/components/ui/skeleton';
import {twMerge} from 'tailwind-merge';

export type ConnectedUserSheetPanelListSkeletonVariantType = 'automation' | 'connection' | 'integration' | 'mcpServer';

interface SkeletonPartsI {
    badges: boolean;
    expand: boolean;
    groupHeader: boolean;
    icon: boolean;
    switch: boolean;
    toolbar: boolean;
    version: boolean;
}

const SKELETON_PARTS: Record<ConnectedUserSheetPanelListSkeletonVariantType, SkeletonPartsI> = {
    automation: {
        badges: false,
        expand: true,
        groupHeader: true,
        icon: false,
        switch: false,
        toolbar: true,
        version: true,
    },
    connection: {
        badges: true,
        expand: false,
        groupHeader: false,
        icon: true,
        switch: false,
        toolbar: false,
        version: false,
    },
    integration: {
        badges: false,
        expand: true,
        groupHeader: false,
        icon: true,
        switch: true,
        toolbar: true,
        version: true,
    },
    mcpServer: {
        badges: false,
        expand: true,
        groupHeader: false,
        icon: false,
        switch: true,
        toolbar: true,
        version: false,
    },
};

interface ConnectedUserSheetPanelListSkeletonProps {
    label: string;
    rowCount?: number;
    variant: ConnectedUserSheetPanelListSkeletonVariantType;
}

const ConnectedUserSheetPanelListSkeleton = ({
    label,
    rowCount = 3,
    variant,
}: ConnectedUserSheetPanelListSkeletonProps) => {
    const parts = SKELETON_PARTS[variant];

    return (
        <div aria-busy="true" aria-label={label} role="status">
            {parts.toolbar && (
                <div className="flex items-center justify-end pb-2" data-testid="skeleton-toolbar">
                    <Skeleton className="h-8 w-40" />
                </div>
            )}

            <ul>
                {Array.from({length: rowCount}, (_, rowIndex) => (
                    <li className="mb-2" key={rowIndex}>
                        {parts.groupHeader && (
                            <div
                                className={
                                    rowIndex === 0
                                        ? 'flex items-center gap-2 pb-2'
                                        : 'flex items-center gap-2 pt-5 pb-2'
                                }
                                data-testid="skeleton-groupHeader"
                            >
                                <Skeleton className="size-5 shrink-0" />

                                <Skeleton className="h-5 w-40" />

                                <Skeleton className="h-5 w-20 rounded-full" />
                            </div>
                        )}

                        <div className="flex items-center justify-between rounded border border-border/50 px-3">
                            {parts.expand && (
                                <Skeleton className="mr-3 size-5 flex-none" data-testid="skeleton-expand" />
                            )}

                            <div className="flex flex-1 items-center gap-x-3 py-3">
                                {parts.icon && <Skeleton className="size-6 shrink-0" data-testid="skeleton-icon" />}

                                <div className="flex flex-col items-start justify-center gap-y-1">
                                    <div className="flex min-h-6 items-center">
                                        <Skeleton className="h-5 w-48" />
                                    </div>

                                    <div className="flex min-h-6 items-center">
                                        <Skeleton className="h-4 w-32" />
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-center gap-x-2">
                                {parts.version && (
                                    <div className="flex w-28 shrink-0 justify-start" data-testid="skeleton-version">
                                        <Skeleton className="h-5 w-9 rounded-full" />
                                    </div>
                                )}

                                <div
                                    className={twMerge(
                                        'flex shrink-0 flex-col items-end gap-y-1',
                                        !parts.badges && 'w-72'
                                    )}
                                >
                                    <div className="flex min-h-6 items-center gap-2">
                                        {parts.switch && (
                                            <Skeleton
                                                className="h-[1.15rem] w-8 rounded-full"
                                                data-testid="skeleton-switch"
                                            />
                                        )}

                                        {parts.badges && (
                                            <div className="flex items-center gap-2" data-testid="skeleton-badges">
                                                <Skeleton className="h-5 w-20 rounded-full" />

                                                <Skeleton className="h-5 w-20 rounded-full" />
                                            </div>
                                        )}
                                    </div>

                                    <div className="flex min-h-6 items-center" data-testid="skeleton-date">
                                        <Skeleton className="h-4 w-36" />
                                    </div>
                                </div>

                                <div
                                    className="flex size-9 shrink-0 items-center justify-center"
                                    data-testid="skeleton-menu"
                                >
                                    <Skeleton className="size-5" />
                                </div>
                            </div>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default ConnectedUserSheetPanelListSkeleton;
