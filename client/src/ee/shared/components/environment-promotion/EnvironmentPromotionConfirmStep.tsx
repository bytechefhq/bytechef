import {Checkbox} from '@/components/ui/checkbox';
import {PromotionChangeKind} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {EnvironmentPromotionPreviewType, getPromotionChangeRows} from './hooks/useEnvironmentPromotionDialog';

export interface EnvironmentPromotionConfirmStepProps {
    destructiveChangeCount: number;
    isDestructiveAcknowledged: boolean;
    onDestructiveAcknowledgedChange: (value: boolean) => void;
    preview: EnvironmentPromotionPreviewType;
    targetLabel: string;
}

const EnvironmentPromotionConfirmStep = ({
    destructiveChangeCount,
    isDestructiveAcknowledged,
    onDestructiveAcknowledgedChange,
    preview,
    targetLabel,
}: EnvironmentPromotionConfirmStepProps) => {
    const changeCounts = useMemo(() => {
        const changeRows = getPromotionChangeRows(preview);

        const countByKind = (kind: PromotionChangeKind) => changeRows.filter((row) => row.kind === kind).length;

        return [
            {kind: PromotionChangeKind.Create, label: `${countByKind(PromotionChangeKind.Create)} to create`},
            {kind: PromotionChangeKind.Update, label: `${countByKind(PromotionChangeKind.Update)} to update`},
            {kind: PromotionChangeKind.Delete, label: `${countByKind(PromotionChangeKind.Delete)} to delete`},
        ];
    }, [preview]);

    const willCreateTarget = preview.existingTargetId == null;

    return (
        <div className="space-y-4">
            <p className="text-sm">
                {willCreateTarget
                    ? `Promoting creates a new counterpart in ${targetLabel}.`
                    : `Promoting updates ${preview.existingTargetName} in ${targetLabel}.`}
            </p>

            <ul className="space-y-1 text-sm text-content-neutral-secondary">
                {changeCounts.map((changeCount) => (
                    <li key={changeCount.kind}>{changeCount.label}</li>
                ))}
            </ul>

            {destructiveChangeCount > 0 && (
                <fieldset className="flex items-start gap-2 border-0">
                    <Checkbox
                        checked={isDestructiveAcknowledged}
                        id="environment-promotion-destructive-acknowledgement"
                        onCheckedChange={(checked) => onDestructiveAcknowledgedChange(checked === true)}
                    />

                    <label
                        className="text-sm text-content-destructive-primary"
                        htmlFor="environment-promotion-destructive-acknowledgement"
                    >
                        {`I understand ${destructiveChangeCount} change(s) delete or overwrite data in ${targetLabel}`}
                    </label>
                </fieldset>
            )}
        </div>
    );
};

export default EnvironmentPromotionConfirmStep;
