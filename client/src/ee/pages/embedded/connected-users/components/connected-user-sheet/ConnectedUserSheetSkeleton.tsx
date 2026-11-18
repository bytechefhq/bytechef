import {Skeleton} from '@/components/ui/skeleton';
import ConnectedUserSheetPanelListSkeleton from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelListSkeleton';
import {twMerge} from 'tailwind-merge';

const PROFILE_ROW_COUNT = 6;

const TAB_LABEL_WIDTHS = ['w-20', 'w-20', 'w-20', 'w-20', 'w-32', 'w-32'];

const ConnectedUserSheetSkeleton = () => (
    <div
        aria-busy="true"
        aria-label="Loading connected user"
        className="flex min-h-full w-full shrink-0 flex-col space-y-10 rounded-lg bg-background p-3"
        role="status"
    >
        <div className="w-full space-y-2">
            <Skeleton className="h-6 w-20" />

            <div className="space-y-2">
                {Array.from({length: PROFILE_ROW_COUNT}, (_, rowIndex) => (
                    <div className="flex w-6/12 gap-x-4" key={rowIndex}>
                        <Skeleton className="h-5 flex-1" />

                        <Skeleton className="h-5 flex-1" />
                    </div>
                ))}
            </div>
        </div>

        <div className="flex w-full flex-col gap-2">
            <div className="inline-flex h-9 w-fit items-center gap-x-1 rounded-lg bg-muted p-[3px]">
                {TAB_LABEL_WIDTHS.map((tabLabelWidth, tabIndex) => (
                    <div className="flex h-full items-center px-2" key={tabIndex}>
                        <Skeleton className={twMerge('h-4', tabLabelWidth)} />
                    </div>
                ))}
            </div>

            <ConnectedUserSheetPanelListSkeleton label="Loading integrations" variant="integration" />
        </div>
    </div>
);

export default ConnectedUserSheetSkeleton;
