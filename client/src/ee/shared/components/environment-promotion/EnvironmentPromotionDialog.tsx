import {Dialog, DialogContent, type DialogStepI, DialogWizard, useDialogSteps} from '@/components/Dialog';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {PromotionResourceType} from '@/shared/middleware/graphql';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {RocketIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

import EnvironmentPromotionChangesStep from './EnvironmentPromotionChangesStep';
import EnvironmentPromotionConfirmStep from './EnvironmentPromotionConfirmStep';
import EnvironmentPromotionConnectionsStep from './EnvironmentPromotionConnectionsStep';
import EnvironmentPromotionDialogFooter from './EnvironmentPromotionDialogFooter';
import EnvironmentPromotionTargetStep from './EnvironmentPromotionTargetStep';
import {useEnvironmentPromotionDialog} from './hooks/useEnvironmentPromotionDialog';

export interface EnvironmentPromotionDialogProps {
    onClose: () => void;
    onPromoted?: (result: {created: boolean; targetEnvironmentId: number; targetId: string}) => void;
    resourceType: PromotionResourceType;
    sourceEnvironmentId: number;
    sourceId: string;
    sourceName: string;
    targetListPath: string;
}

interface EnvironmentPromotionStepContentProps {
    promotionDialog: ReturnType<typeof useEnvironmentPromotionDialog>;
}

const EnvironmentPromotionStepContent = ({promotionDialog}: EnvironmentPromotionStepContentProps) => {
    const {completedStepIds, currentStep, goToNextStep, isPending} = useDialogSteps();

    const {
        completeReturnToReview,
        destructiveChangeCount,
        handleTargetConnectionIdChange,
        handleTargetEnvironmentIdChange,
        isDestructiveAcknowledged,
        isPreviewError,
        isPreviewLoading,
        isPreviewStale,
        isReturnToReviewPending,
        mappings,
        preview,
        previewError,
        setDestructiveAcknowledged,
        targetEnvironmentId,
        targetEnvironmentLabel,
        targetEnvironmentOptions,
        unresolvedConnectionCount,
    } = promotionDialog;

    useEffect(() => {
        if (
            !isReturnToReviewPending ||
            isPending ||
            currentStep.id !== 'target' ||
            currentStep.canProceed === false ||
            completedStepIds.size > 0
        ) {
            return;
        }

        completeReturnToReview();

        goToNextStep().catch(() => undefined);
    }, [completeReturnToReview, completedStepIds, currentStep, goToNextStep, isPending, isReturnToReviewPending]);

    if (currentStep.id === 'target' || !preview) {
        return (
            <EnvironmentPromotionTargetStep
                isPreviewError={isPreviewError}
                isPreviewLoading={isPreviewLoading}
                onTargetEnvironmentIdChange={handleTargetEnvironmentIdChange}
                preview={preview}
                previewError={previewError}
                targetEnvironmentId={targetEnvironmentId}
                targetEnvironmentOptions={targetEnvironmentOptions}
            />
        );
    }

    if (currentStep.id === 'changes') {
        return (
            <EnvironmentPromotionChangesStep
                isPreviewStale={isPreviewStale}
                preview={preview}
                targetLabel={targetEnvironmentLabel}
            />
        );
    }

    if (currentStep.id === 'connections') {
        return (
            <EnvironmentPromotionConnectionsStep
                mappings={mappings}
                onTargetConnectionIdChange={handleTargetConnectionIdChange}
                preview={preview}
                unresolvedConnectionCount={unresolvedConnectionCount}
            />
        );
    }

    return (
        <EnvironmentPromotionConfirmStep
            destructiveChangeCount={destructiveChangeCount}
            isDestructiveAcknowledged={isDestructiveAcknowledged}
            onDestructiveAcknowledgedChange={setDestructiveAcknowledged}
            preview={preview}
            targetLabel={targetEnvironmentLabel}
        />
    );
};

const EnvironmentPromotionDialog = ({
    onClose,
    onPromoted,
    resourceType,
    sourceEnvironmentId,
    sourceId,
    sourceName,
    targetListPath,
}: EnvironmentPromotionDialogProps) => {
    const [open, setOpen] = useState(true);

    const promotionDialog = useEnvironmentPromotionDialog({resourceType, sourceEnvironmentId, sourceId});

    const navigate = useNavigate();

    const {
        destructiveChangeCount,
        handlePromote,
        isDestructiveAcknowledged,
        isPreviewError,
        isPreviewFetching,
        isPromotePending,
        preview,
        targetEnvironmentId,
        wizardResetKey,
    } = promotionDialog;

    const hasConnections = (preview?.connections.length || 0) > 0;

    const steps = useMemo<DialogStepI[]>(
        () => [
            {canProceed: !!preview && !isPreviewError && !isPreviewFetching, id: 'target', label: 'Target'},
            {id: 'changes', label: 'Review changes'},
            ...(hasConnections ? [{id: 'connections', label: 'Connections'}] : []),
            {
                canProceed:
                    !isPromotePending &&
                    !isPreviewFetching &&
                    (destructiveChangeCount === 0 || isDestructiveAcknowledged),
                id: 'confirm',
                label: 'Confirm',
            },
        ],
        [
            destructiveChangeCount,
            hasConnections,
            isDestructiveAcknowledged,
            isPreviewError,
            isPreviewFetching,
            isPromotePending,
            preview,
        ]
    );

    const closeDialog = () => {
        setOpen(false);

        onClose();
    };

    const handlePromoteComplete = async () => {
        const result = await handlePromote();

        if (!result || targetEnvironmentId == null) {
            return;
        }

        const targetConfig = ENVIRONMENT_CONFIGS[targetEnvironmentId];
        const targetLabel = targetConfig ? targetConfig.label : `environment ${targetEnvironmentId}`;

        const descriptionLines = [
            result.created
                ? `A new "${sourceName}" counterpart was created disabled in ${targetLabel}; enable it after reviewing its connections.`
                : `The existing "${sourceName}" counterpart in ${targetLabel} was updated.`,
        ];

        if (result.created && result.targetUrl) {
            descriptionLines.push(`New URL: ${result.targetUrl}`);
        }

        if (result.unresolvedConnectionIds.length > 0) {
            descriptionLines.push(`${result.unresolvedConnectionIds.length} connections still need a binding.`);
        }

        const projectId = preview?.projects[0]?.projectId;

        const navigationPath =
            resourceType === PromotionResourceType.ProjectDeployment && projectId
                ? `${targetListPath}?projectId=${projectId}`
                : targetListPath;

        const targetId = result.targetId;

        toast.success(`Promoted to ${targetLabel}`, {
            action: {
                label: `View in ${targetLabel}`,
                onClick: () => {
                    environmentStore.getState().setCurrentEnvironmentId(targetEnvironmentId);

                    void navigate(navigationPath, {
                        state:
                            resourceType === PromotionResourceType.ProjectDeployment
                                ? {projectDeploymentId: +targetId}
                                : undefined,
                    });
                },
            },
            description: descriptionLines.join(' '),
        });

        onPromoted?.({created: result.created, targetEnvironmentId, targetId: result.targetId});

        closeDialog();
    };

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (!nextOpen) {
                    closeDialog();
                }
            }}
            open={open}
        >
            <DialogContent hasSidebar>
                <DialogWizard
                    footer={<EnvironmentPromotionDialogFooter isPromotePending={isPromotePending} />}
                    icon={<RocketIcon />}
                    onComplete={handlePromoteComplete}
                    resetKey={wizardResetKey}
                    steps={steps}
                    title={`Promote "${sourceName}"`}
                >
                    <EnvironmentPromotionStepContent promotionDialog={promotionDialog} />
                </DialogWizard>
            </DialogContent>
        </Dialog>
    );
};

export default EnvironmentPromotionDialog;
