import Badge from '@/components/Badge/Badge';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';

import {EnvironmentPromotionPreviewConnectionType} from './hooks/useEnvironmentPromotionDialog';

export const UNRESOLVED_CONNECTION_VALUE = 'UNRESOLVED';

export interface EnvironmentPromotionConnectionRowProps {
    connection: EnvironmentPromotionPreviewConnectionType;
    onTargetConnectionIdChange: (sourceConnectionId: string, targetConnectionId: string | undefined) => void;
    targetConnectionId: string | undefined;
}

const EnvironmentPromotionConnectionRow = ({
    connection,
    onTargetConnectionIdChange,
    targetConnectionId,
}: EnvironmentPromotionConnectionRowProps) => {
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
                    {connection.suggestedTargetConnectionId == null && (
                        <SelectItem value={UNRESOLVED_CONNECTION_VALUE}>Unresolved</SelectItem>
                    )}

                    {connection.targetCandidates.map((targetCandidate) => (
                        <SelectItem key={targetCandidate.connectionId} value={targetCandidate.connectionId}>
                            {targetCandidate.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

export default EnvironmentPromotionConnectionRow;
