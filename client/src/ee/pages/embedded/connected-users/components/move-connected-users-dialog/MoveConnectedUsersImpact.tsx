import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import MoveConnectedUsersSkippedList, {
    type SkippedUserDisplayI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';
import {InfoIcon} from 'lucide-react';

import type {MoveConnectedUsersSkippedI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface MoveConnectedUsersImpactProps {
    impact?: Array<{
        affectedConnectedUserCount: number;
        awaitsConfiguration: boolean;
        label: string;
        newRequiredConnections: string[];
        newRequiredInputs: string[];
    }>;
    mcpServerCount?: number;
    previewError?: boolean;
    previewSkipped?: MoveConnectedUsersSkippedI[];
    toSkippedUser: (skippedUser: MoveConnectedUsersSkippedI) => SkippedUserDisplayI;
}

const MoveConnectedUsersImpact = ({
    impact,
    mcpServerCount,
    previewError,
    previewSkipped,
    toSkippedUser,
}: MoveConnectedUsersImpactProps) => (
    <>
        {impact?.map((impactItem) => (
            <Alert key={impactItem.label} variant="default">
                <InfoIcon />

                <AlertTitle>
                    {`${impactItem.label}: affects ${impactItem.affectedConnectedUserCount} ${impactItem.affectedConnectedUserCount === 1 ? 'user' : 'users'}`}
                </AlertTitle>

                {(impactItem.newRequiredConnections.length > 0 ||
                    impactItem.newRequiredInputs.length > 0 ||
                    impactItem.awaitsConfiguration) && (
                    <AlertDescription>
                        {impactItem.newRequiredConnections.length > 0 && (
                            <p>{`New required connections: ${impactItem.newRequiredConnections.join(', ')}`}</p>
                        )}

                        {impactItem.newRequiredInputs.length > 0 && (
                            <p>{`New required inputs: ${impactItem.newRequiredInputs.join(', ')}`}</p>
                        )}

                        {impactItem.awaitsConfiguration && (
                            <p>Stays disabled until the configuration sets its internal-only inputs</p>
                        )}
                    </AlertDescription>
                )}
            </Alert>
        ))}

        {!!mcpServerCount && (
            <Alert variant="default">
                <InfoIcon />

                <AlertDescription>
                    {`${mcpServerCount} ${mcpServerCount === 1 ? 'MCP server uses' : 'MCP servers use'} one of these configurations. Their tools follow the configuration each user lands on.`}
                </AlertDescription>
            </Alert>
        )}

        {previewError && (
            <p className="text-sm text-content-destructive-primary" role="alert">
                Could not load the move preview.
            </p>
        )}

        {!!previewSkipped?.length && (
            <div className="space-y-1 text-sm">
                <p className="font-medium">Users that would be skipped</p>

                <MoveConnectedUsersSkippedList skippedUsers={previewSkipped.map(toSkippedUser)} />
            </div>
        )}
    </>
);

export default MoveConnectedUsersImpact;
