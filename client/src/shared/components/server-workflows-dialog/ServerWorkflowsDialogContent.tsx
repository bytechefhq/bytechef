import {
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogNextButton,
    DialogPreviousButton,
    DialogSidebar,
    type DialogStepI,
    DialogStepIndicator,
    DialogSteps,
    DialogStepsProvider,
    useDialogSteps,
} from '@/components/Dialog';
import LoadingDots from '@/components/LoadingDots';
import {ReactElement, ReactNode, useMemo} from 'react';

interface ServerWorkflowsDialogContentProps {
    basicStepContent: ReactNode;
    canProceedFromBasicStep: boolean;
    description: string;
    icon: ReactElement;
    isEditMode: boolean;
    isSubmitPending?: boolean;
    noEligibleWorkflowsMessage: string;
    onComplete: () => void | Promise<void>;
    selectedWorkflowIds: string[];
    title: string;
    validateBasicStep: () => boolean | Promise<boolean>;
    workflowCount?: number;
    workflowsList: ReactNode;
}

interface ServerWorkflowsDialogStepContentProps {
    basicStepContent: ReactNode;
    workflowsStepContent: ReactNode;
}

const ServerWorkflowsDialogStepContent = ({
    basicStepContent,
    workflowsStepContent,
}: ServerWorkflowsDialogStepContentProps) => {
    const {currentStep} = useDialogSteps();

    return <>{currentStep.id === 'workflows' ? workflowsStepContent : basicStepContent}</>;
};

const ServerWorkflowsDialogContent = ({
    basicStepContent,
    canProceedFromBasicStep,
    description,
    icon,
    isEditMode,
    isSubmitPending = false,
    noEligibleWorkflowsMessage,
    onComplete,
    selectedWorkflowIds,
    title,
    validateBasicStep,
    workflowCount,
    workflowsList,
}: ServerWorkflowsDialogContentProps) => {
    const steps = useMemo<DialogStepI[]>(
        () => [
            {canProceed: canProceedFromBasicStep, id: 'basic', label: 'Basic'},
            {
                canProceed: selectedWorkflowIds.length > 0 && !!workflowCount && !isSubmitPending,
                id: 'workflows',
                label: 'Workflows',
            },
        ],
        [canProceedFromBasicStep, isSubmitPending, selectedWorkflowIds.length, workflowCount]
    );

    const workflowsStepContent = useMemo(() => {
        if (workflowCount === undefined) {
            return (
                <div className="flex justify-center py-12">
                    <LoadingDots />
                </div>
            );
        }

        return (
            <div className="space-y-5">
                {workflowCount === 0 ? (
                    <p className="text-sm text-content-neutral-secondary">{noEligibleWorkflowsMessage}</p>
                ) : (
                    workflowsList
                )}
            </div>
        );
    }, [noEligibleWorkflowsMessage, workflowCount, workflowsList]);

    const validateStep = (step: DialogStepI) => (step.id === 'basic' ? validateBasicStep() : true);

    const submitLabel = isEditMode ? 'Update' : 'Add';

    return (
        <DialogContent hasSidebar onInteractOutside={(event) => event.preventDefault()}>
            <DialogStepsProvider
                defaultStepId={isEditMode ? 'workflows' : 'basic'}
                onComplete={onComplete}
                steps={steps}
                validateStep={validateStep}
            >
                <DialogSidebar description={description} icon={icon} title={title}>
                    <DialogSteps />

                    <DialogStepIndicator />
                </DialogSidebar>

                <DialogMain>
                    <DialogHeader dialogDescription={description} dialogTitle={title} />

                    <DialogBody>
                        <ServerWorkflowsDialogStepContent
                            basicStepContent={basicStepContent}
                            workflowsStepContent={workflowsStepContent}
                        />
                    </DialogBody>

                    <DialogFooter startContent={<DialogCancelButton />}>
                        <DialogPreviousButton className="lg:hidden" />

                        <DialogNextButton
                            isPending={isSubmitPending}
                            label="Next"
                            lastStepLabel={isSubmitPending ? 'Saving...' : submitLabel}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogStepsProvider>
        </DialogContent>
    );
};

export default ServerWorkflowsDialogContent;
