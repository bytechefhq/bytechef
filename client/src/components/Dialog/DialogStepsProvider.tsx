import {useControllableState} from 'radix-ui/internal';
import {type ReactNode, createContext, useCallback, useEffect, useMemo, useRef, useState} from 'react';

interface DialogStepI {
    canProceed?: boolean;
    id: string;
    label: string;
}

interface DialogStepStatusI {
    isCompleted: boolean;
    isCurrent: boolean;
    isReachable: boolean;
}

interface DialogStepsContextI {
    completedStepIds: ReadonlySet<string>;
    currentStep: DialogStepI;
    currentStepIndex: number;
    getStepStatus: (stepId: string) => DialogStepStatusI;
    goToNextStep: () => Promise<void>;
    goToPreviousStep: () => void;
    goToStep: (stepId: string) => void;
    isFirstStep: boolean;
    isLastStep: boolean;
    isPending: boolean;
    steps: DialogStepI[];
}

interface DialogStepsProviderProps {
    children: ReactNode;
    currentStepId?: string;
    defaultStepId?: string;
    onComplete?: () => void | Promise<void>;
    onCurrentStepChange?: (stepId: string) => void;
    steps: DialogStepI[];
    validateStep?: (step: DialogStepI) => boolean | Promise<boolean>;
}

const DialogStepsContext = createContext<DialogStepsContextI | null>(null);

function resolveCurrentStepIndex(steps: DialogStepI[], selectedStepId: string, firstIncompleteIndex: number) {
    const selectedStepIndex = steps.findIndex((step) => step.id === selectedStepId);

    if (selectedStepIndex !== -1) {
        return selectedStepIndex;
    }

    return Math.max(firstIncompleteIndex, 0);
}

function DialogStepsProvider({
    children,
    currentStepId,
    defaultStepId,
    onComplete,
    onCurrentStepChange,
    steps,
    validateStep,
}: DialogStepsProviderProps) {
    if (steps.length === 0) {
        throw new Error('DialogStepsProvider requires at least one step');
    }

    const [completedStepIds, setCompletedStepIds] = useState<ReadonlySet<string>>(() => new Set());
    const [isPending, setIsPending] = useState(false);

    const isPendingRef = useRef(false);

    const [selectedStepId, setSelectedStepId] = useControllableState({
        caller: 'DialogStepsProvider',
        defaultProp: defaultStepId || steps[0]?.id || '',
        onChange: onCurrentStepChange,
        prop: currentStepId,
    });

    const firstIncompleteIndex = steps.findIndex((step) => !completedStepIds.has(step.id));

    const currentStepIndex = resolveCurrentStepIndex(steps, selectedStepId, firstIncompleteIndex);

    const currentStep = steps[currentStepIndex];
    const isFirstStep = currentStepIndex === 0;
    const isLastStep = currentStepIndex === steps.length - 1;

    const getStepStatus = useCallback(
        (stepId: string): DialogStepStatusI => {
            const stepIndex = steps.findIndex((step) => step.id === stepId);
            const isCompleted = completedStepIds.has(stepId);

            return {
                isCompleted,
                isCurrent: stepIndex === currentStepIndex,
                isReachable: stepIndex !== -1 && (isCompleted || stepIndex === firstIncompleteIndex),
            };
        },
        [completedStepIds, currentStepIndex, firstIncompleteIndex, steps]
    );

    const goToStep = useCallback(
        (stepId: string) => {
            const {isCurrent, isReachable} = getStepStatus(stepId);

            if (isPendingRef.current || isCurrent || !isReachable) {
                return;
            }

            setSelectedStepId(stepId);
        },
        [getStepStatus, setSelectedStepId]
    );

    const goToPreviousStep = useCallback(() => {
        if (isPendingRef.current || currentStepIndex === 0) {
            return;
        }

        setSelectedStepId(steps[currentStepIndex - 1].id);
    }, [currentStepIndex, setSelectedStepId, steps]);

    const goToNextStep = useCallback(async () => {
        if (isPendingRef.current || currentStep.canProceed === false) {
            return;
        }

        isPendingRef.current = true;

        setIsPending(true);

        try {
            const isValid = validateStep ? await validateStep(currentStep) : true;

            if (!isValid) {
                return;
            }

            if (isLastStep) {
                await onComplete?.();
            } else {
                setSelectedStepId(steps[currentStepIndex + 1].id);
            }

            setCompletedStepIds((previousCompletedStepIds) => {
                const nextCompletedStepIds = new Set(previousCompletedStepIds);

                nextCompletedStepIds.add(currentStep.id);

                return nextCompletedStepIds;
            });
        } finally {
            isPendingRef.current = false;

            setIsPending(false);
        }
    }, [currentStep, currentStepIndex, isLastStep, onComplete, setSelectedStepId, steps, validateStep]);

    const contextValue = useMemo<DialogStepsContextI>(
        () => ({
            completedStepIds,
            currentStep,
            currentStepIndex,
            getStepStatus,
            goToNextStep,
            goToPreviousStep,
            goToStep,
            isFirstStep,
            isLastStep,
            isPending,
            steps,
        }),
        [
            completedStepIds,
            currentStep,
            currentStepIndex,
            getStepStatus,
            goToNextStep,
            goToPreviousStep,
            goToStep,
            isFirstStep,
            isLastStep,
            isPending,
            steps,
        ]
    );

    useEffect(() => {
        if (currentStepId === undefined && selectedStepId !== currentStep.id) {
            setSelectedStepId(currentStep.id);
        }
    }, [currentStep, currentStepId, selectedStepId, setSelectedStepId]);

    return <DialogStepsContext value={contextValue}>{children}</DialogStepsContext>;
}

DialogStepsProvider.displayName = 'DialogStepsProvider';

export {DialogStepsContext, DialogStepsProvider};
export type {DialogStepI, DialogStepsContextI, DialogStepStatusI, DialogStepsProviderProps};
