import {Alert, AlertTitle} from '@/components/ui/alert';
import MoveConnectedUsersSkippedList, {
    type SkippedUserDisplayI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';
import {InfoIcon} from 'lucide-react';

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
        <Alert variant="default">
            <InfoIcon />

            <AlertTitle>
                {`${result.movedConnectedUserIds.length} moved, ${result.needsAttentionConnectedUserIds.length} ${result.needsAttentionConnectedUserIds.length === 1 ? 'needs' : 'need'} attention`}
            </AlertTitle>
        </Alert>

        {result.skipped.length > 0 && (
            <MoveConnectedUsersSkippedList skippedUsers={result.skipped.map(toSkippedUser)} />
        )}
    </div>
);

export default MoveConnectedUsersResult;
