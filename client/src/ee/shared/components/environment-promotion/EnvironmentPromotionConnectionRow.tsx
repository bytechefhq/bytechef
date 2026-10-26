import Badge from '@/components/Badge/Badge';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';

import {EnvironmentPromotionPreviewConnectionType} from './hooks/useEnvironmentPromotionDialog';

export const UNRESOLVED_CONNECTION_VALUE = 'UNRESOLVED';

export interface EnvironmentPromotionConnectionRowProps {
    connection: EnvironmentPromotionPreviewConnectionType;
    onTargetConnectionIdChange: (sourceConnectionId: string, targetConnectionId: string | undefined) => void;
    targetConnectionId: string | undefined;
    targetEnvironmentId: number;
    workspaceId: number;
}

const EnvironmentPromotionConnectionRow = ({
    connection,
    onTargetConnectionIdChange,
    targetConnectionId,
    targetEnvironmentId,
    workspaceId,
}: EnvironmentPromotionConnectionRowProps) => {
    const {data: targetConnections} = useGetWorkspaceConnectionsQuery({
        componentName: connection.componentName,
        connectionVersion: connection.connectionVersion,
        environmentId: targetEnvironmentId,
        id: workspaceId,
    });

    return (
        <div className="flex items-center justify-between gap-4 border-b py-3 last:border-b-0">
            <div className="flex flex-col gap-1">
                <span className="text-sm font-medium">{connection.sourceConnectionName}</span>

                <span className="text-xs text-content-neutral-secondary">{connection.componentName}</span>

                {connection.usedBy.length > 0 && (
                    <div className="flex flex-wrap gap-1">
                        {connection.usedBy.map((usedByName) => (
                            <Badge key={usedByName} label={usedByName} styleType="secondary-outline" />
                        ))}
                    </div>
                )}
            </div>

            <Select
                onValueChange={(value) =>
                    onTargetConnectionIdChange(
                        connection.sourceConnectionId,
                        value === UNRESOLVED_CONNECTION_VALUE ? undefined : value
                    )
                }
                value={targetConnectionId ?? UNRESOLVED_CONNECTION_VALUE}
            >
                <SelectTrigger aria-label={`Target connection for ${connection.sourceConnectionName}`} className="w-64">
                    <SelectValue placeholder="Choose connection..." />
                </SelectTrigger>

                <SelectContent>
                    <SelectItem value={UNRESOLVED_CONNECTION_VALUE}>Unresolved</SelectItem>

                    {targetConnections?.map((targetConnection) => (
                        <SelectItem key={targetConnection.id} value={targetConnection.id!.toString()}>
                            {targetConnection.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

export default EnvironmentPromotionConnectionRow;
