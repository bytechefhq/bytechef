import type {
    MoveConnectedUsersResultI,
    MoveConnectedUsersSkippedI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface MoveConnectedUsersResultProps {
    result: MoveConnectedUsersResultI;
    toSkippedLine: (skippedUser: MoveConnectedUsersSkippedI) => string;
}

const MoveConnectedUsersResult = ({result, toSkippedLine}: MoveConnectedUsersResultProps) => (
    <div className="flex flex-col gap-2">
        <p className="text-sm font-medium">
            {`${result.movedConnectedUserIds.length} moved, ${result.needsAttentionConnectedUserIds.length} need attention`}
        </p>

        {result.skipped.map((skippedUser, index) => (
            <p className="text-sm text-content-neutral-secondary" key={`${skippedUser.connectedUserId}-${index}`}>
                {toSkippedLine(skippedUser)}
            </p>
        ))}
    </div>
);

export default MoveConnectedUsersResult;
