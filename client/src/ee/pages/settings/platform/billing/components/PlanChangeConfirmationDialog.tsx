import AlertDialog, {ConfirmVariantType} from '@/components/AlertDialog';

type PlanChangeDirectionType = 'downgrade' | 'upgrade';

interface PlanChangeCopyI {
    confirmButtonVariant: ConfirmVariantType;
    confirmLabel: string;
    effect: string;
    pendingLabel: string;
    title: string;
    verb: string;
}

const PLAN_CHANGE_COPY: Record<PlanChangeDirectionType, PlanChangeCopyI> = {
    downgrade: {
        confirmButtonVariant: 'destructive',
        confirmLabel: 'Confirm downgrade',
        effect: 'This change will take effect at the end of your current billing period. You will retain access to your current plan features until then.',
        pendingLabel: 'Downgrading…',
        title: 'Downgrade',
        verb: 'downgrading',
    },
    upgrade: {
        confirmButtonVariant: 'default',
        confirmLabel: 'Upgrade now',
        effect: 'You will be charged immediately for the prorated cost for the remainder of your current billing period. This action cannot be undone.',
        pendingLabel: 'Upgrading…',
        title: 'Upgrade',
        verb: 'upgrading',
    },
};

interface PlanChangeConfirmationDialogPropsI {
    currentPlanName?: string;
    direction: PlanChangeDirectionType;
    isPending: boolean;
    newPlanName: string;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const PlanChangeConfirmationDialog = ({
    currentPlanName,
    direction,
    isPending,
    newPlanName,
    onClose,
    onConfirm,
    open,
}: PlanChangeConfirmationDialogPropsI) => {
    const copy = PLAN_CHANGE_COPY[direction];

    return (
        <AlertDialog
            cancelLabel="Keep current plan"
            confirmButtonVariant={copy.confirmButtonVariant}
            confirmLabel={isPending ? copy.pendingLabel : copy.confirmLabel}
            description={
                <>
                    {currentPlanName && (
                        <span className="mb-2 block">
                            {`You are ${copy.verb} from `}

                            <strong>{currentPlanName}</strong>

                            {' to '}

                            <strong>{newPlanName}</strong>

                            {'.'}
                        </span>
                    )}

                    <span className="block">{copy.effect}</span>
                </>
            }
            isPending={isPending}
            onCancel={onClose}
            onConfirm={onConfirm}
            open={open}
            title={`${copy.title} to ${newPlanName}?`}
        />
    );
};

export default PlanChangeConfirmationDialog;
