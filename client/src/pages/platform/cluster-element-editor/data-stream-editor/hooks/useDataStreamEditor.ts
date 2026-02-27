import {useCallback, useState} from 'react';

const TOTAL_STEPS = 3;

interface UseDataStreamEditorResultI {
    currentStep: number;
    handleGoToStep: (step: number) => void;
    handleNext: () => void;
    handlePrevious: () => void;
}

export default function useDataStreamEditor(): UseDataStreamEditorResultI {
    const [currentStep, setCurrentStep] = useState(0);

    const handleNext = useCallback(() => {
        setCurrentStep((previousStep) => Math.min(previousStep + 1, TOTAL_STEPS - 1));
    }, []);

    const handlePrevious = useCallback(() => {
        setCurrentStep((previousStep) => Math.max(previousStep - 1, 0));
    }, []);

    const handleGoToStep = useCallback((step: number) => {
        if (step >= 0 && step < TOTAL_STEPS) {
            setCurrentStep(step);
        }
    }, []);

    return {
        currentStep,
        handleGoToStep,
        handleNext,
        handlePrevious,
    };
}
