import {Skeleton} from '@/components/ui/skeleton';
import ConnectedUserSheetPanelListSkeleton from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelListSkeleton';

const PROFILE_ROW_COUNT = 4;
const TAB_COUNT = 6;

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

        <div className="w-full space-y-4">
            <div className="flex w-full gap-x-2 border-b border-border pb-2.5">
                {Array.from({length: TAB_COUNT}, (_, tabIndex) => (
                    <Skeleton className="h-5 flex-1" key={tabIndex} />
                ))}
            </div>

            <ConnectedUserSheetPanelListSkeleton label="Loading integrations" />
        </div>
    </div>
);

export default ConnectedUserSheetSkeleton;
