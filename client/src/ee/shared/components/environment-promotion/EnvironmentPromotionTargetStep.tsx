import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {AlertTriangleIcon} from 'lucide-react';

import {EnvironmentOptionI, EnvironmentPromotionPreviewType} from './hooks/useEnvironmentPromotionDialog';

export interface EnvironmentPromotionTargetStepProps {
    isPreviewError: boolean;
    isPreviewLoading: boolean;
    onTargetEnvironmentIdChange: (targetEnvironmentId: number) => void;
    preview: EnvironmentPromotionPreviewType | undefined;
    previewError: unknown;
    targetEnvironmentId: number | undefined;
    targetEnvironmentOptions: EnvironmentOptionI[];
}

const EnvironmentPromotionTargetStep = ({
    isPreviewError,
    isPreviewLoading,
    onTargetEnvironmentIdChange,
    preview,
    previewError,
    targetEnvironmentId,
    targetEnvironmentOptions,
}: EnvironmentPromotionTargetStepProps) => {
    const willCreateTarget = preview ? preview.existingTargetId == null : false;

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <label className="text-sm font-medium" htmlFor="environment-promotion-target-select">
                    Target environment
                </label>

                <Select
                    onValueChange={(value) => onTargetEnvironmentIdChange(+value)}
                    value={targetEnvironmentId != null ? targetEnvironmentId.toString() : undefined}
                >
                    <SelectTrigger id="environment-promotion-target-select">
                        <SelectValue placeholder="Choose target environment..." />
                    </SelectTrigger>

                    <SelectContent>
                        {targetEnvironmentOptions.map((option) => (
                            <SelectItem key={option.id} value={option.id.toString()}>
                                {option.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            {isPreviewLoading && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <LoadingIcon />

                    <span>Loading preview…</span>
                </div>
            )}

            {isPreviewError && (
                <Alert variant="destructive">
                    <AlertTriangleIcon />

                    <AlertTitle>The preview could not be loaded</AlertTitle>

                    <AlertDescription>
                        {previewError instanceof Error ? previewError.message : 'Try another target environment.'}
                    </AlertDescription>
                </Alert>
            )}

            {preview && !isPreviewError && (
                <Badge
                    className="border-transparent"
                    label={willCreateTarget ? 'Will create' : `Will update ${preview.existingTargetName}`}
                    styleType={willCreateTarget ? 'success-outline' : 'warning-outline'}
                />
            )}
        </div>
    );
};

export default EnvironmentPromotionTargetStep;
