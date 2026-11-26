import Button from '@/components/Button/Button';
import {
    DialogCancelButton,
    DialogFooter,
    DialogNextButton,
    DialogPreviousButton,
    useDialogSteps,
} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';

export interface EnvironmentPromotionDialogFooterProps {
    isPromotePending: boolean;
}

const EnvironmentPromotionDialogFooter = ({isPromotePending}: EnvironmentPromotionDialogFooterProps) => {
    const {currentStep, goToNextStep, isLastStep, isPending} = useDialogSteps();

    const handlePromoteClick = () => {
        goToNextStep().catch(() => undefined);
    };

    return (
        <DialogFooter startContent={<DialogCancelButton />}>
            <DialogPreviousButton />

            {isLastStep ? (
                <Button
                    disabled={isPending || isPromotePending || currentStep.canProceed === false}
                    icon={isPromotePending ? <LoadingIcon /> : undefined}
                    label={isPromotePending ? 'Promoting…' : 'Promote'}
                    onClick={handlePromoteClick}
                    type="button"
                />
            ) : (
                <DialogNextButton label="Next" />
            )}
        </DialogFooter>
    );
};

EnvironmentPromotionDialogFooter.displayName = 'EnvironmentPromotionDialogFooter';

export default EnvironmentPromotionDialogFooter;
