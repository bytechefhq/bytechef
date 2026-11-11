import Badge from '@/components/Badge/Badge';
import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {ReactNode} from 'react';

import {
    type ExternalInvocationLogItemType,
    SOURCE_LABELS,
    STATUS_STYLE_TYPES,
    formatCreatedDate,
    targetLabel,
} from '../utils/externalInvocationLabels';

interface ExternalInvocationDetailsDialogProps {
    externalInvocationLog: ExternalInvocationLogItemType;
    onClose: () => void;
}

const DetailRow = ({children, label}: {children: ReactNode; label: string}) => (
    <div className="grid grid-cols-3 gap-2 py-1.5 text-sm">
        <dt className="text-content-neutral-secondary">{label}</dt>

        <dd className="col-span-2 break-all text-content-neutral-primary">{children}</dd>
    </div>
);

const ExternalInvocationDetailsDialog = ({externalInvocationLog, onClose}: ExternalInvocationDetailsDialogProps) => (
    <Dialog onOpenChange={(open) => !open && onClose()} open>
        <DialogContent size="md">
            <DialogMain>
                <DialogHeader
                    description={formatCreatedDate(externalInvocationLog.createdDate)}
                    title={externalInvocationLog.toolName || targetLabel(externalInvocationLog)}
                />

                <DialogBody>
                    <dl className="divide-y divide-stroke-neutral-secondary rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-secondary px-3">
                        <DetailRow label="Status">
                            <Badge
                                className="border-transparent uppercase"
                                label={externalInvocationLog.status}
                                styleType={STATUS_STYLE_TYPES[externalInvocationLog.status] || 'secondary-filled'}
                                weight="semibold"
                            />
                        </DetailRow>

                        <DetailRow label="Target">{targetLabel(externalInvocationLog)}</DetailRow>

                        {externalInvocationLog.componentVersion != null && (
                            <DetailRow label="Component version">{externalInvocationLog.componentVersion}</DetailRow>
                        )}

                        <DetailRow label="Source">
                            {SOURCE_LABELS[externalInvocationLog.source] || externalInvocationLog.source}
                        </DetailRow>

                        <DetailRow label="Duration">{`${externalInvocationLog.durationMs}ms`}</DetailRow>

                        {externalInvocationLog.externalUserId && (
                            <DetailRow label="External user">{externalInvocationLog.externalUserId}</DetailRow>
                        )}

                        {externalInvocationLog.errorType && (
                            <DetailRow label="Error type">{externalInvocationLog.errorType}</DetailRow>
                        )}

                        {externalInvocationLog.errorMessage && (
                            <DetailRow label="Error">
                                <pre className="text-xs whitespace-pre-wrap text-content-destructive-primary">
                                    {externalInvocationLog.errorMessage}
                                </pre>
                            </DetailRow>
                        )}
                    </dl>
                </DialogBody>
            </DialogMain>
        </DialogContent>
    </Dialog>
);

export default ExternalInvocationDetailsDialog;
