import MoveConnectedUsersSkippedList, {
    type SkippedUserDisplayI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';

import type {
    MoveConnectedUsersResultI,
    MoveConnectedUsersSkippedI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface MoveConnectedUsersResultProps {
    result: MoveConnectedUsersResultI;
    toSkippedUser: (skippedUser: MoveConnectedUsersSkippedI) => SkippedUserDisplayI;
}

const MoveConnectedUsersResult = ({result, toSkippedUser}: MoveConnectedUsersResultProps) => (
    <div className="flex flex-col gap-2">
        <p className="text-sm font-medium">
            {`${result.movedConnectedUserIds.length} moved, ${result.needsAttentionConnectedUserIds.length} need attention`}
        </p>

        {result.skipped.length > 0 && (
            <MoveConnectedUsersSkippedList skippedUsers={result.skipped.map(toSkippedUser)} />
        )}
    </div>
);

export default MoveConnectedUsersResult;
