import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {AlertTriangleIcon} from 'lucide-react';

import EnvironmentPromotionConnectionRow from './EnvironmentPromotionConnectionRow';
import {EnvironmentPromotionPreviewType} from './hooks/useEnvironmentPromotionDialog';

export interface EnvironmentPromotionConnectionsStepProps {
    mappings: Record<string, string | undefined>;
    onTargetConnectionIdChange: (sourceConnectionId: string, targetConnectionId: string | undefined) => void;
    preview: EnvironmentPromotionPreviewType;
    unresolvedConnectionCount: number;
}

const EnvironmentPromotionConnectionsStep = ({
    mappings,
    onTargetConnectionIdChange,
    preview,
    unresolvedConnectionCount,
}: EnvironmentPromotionConnectionsStepProps) => {
    const willCreateTarget = preview.existingTargetId == null;

    return (
        <div className="space-y-3">
            {unresolvedConnectionCount > 0 && (
                <Alert variant="warning">
                    <AlertTriangleIcon />

                    <AlertTitle>Unbound connections</AlertTitle>

                    <AlertDescription>
                        {willCreateTarget
                            ? `${unresolvedConnectionCount} connection(s) are unresolved; bind them in the new target before enabling it.`
                            : `${unresolvedConnectionCount} connection(s) are unresolved, so the affected nodes in the existing target are published without a connection.`}
                    </AlertDescription>
                </Alert>
            )}

            <fieldset className="space-y-1 border-0">
                <legend className="text-sm font-medium">Connections</legend>

                {preview.connections.map((connection) => (
                    <EnvironmentPromotionConnectionRow
                        connection={connection}
                        key={connection.sourceConnectionId}
                        onTargetConnectionIdChange={onTargetConnectionIdChange}
                        targetConnectionId={mappings[connection.sourceConnectionId]}
                    />
                ))}
            </fieldset>
        </div>
    );
};

export default EnvironmentPromotionConnectionsStep;
