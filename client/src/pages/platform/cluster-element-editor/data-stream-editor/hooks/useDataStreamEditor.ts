import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCallback, useMemo, useState} from 'react';

const TOTAL_STEPS = 3;

interface UseDataStreamEditorResultI {
    configuredSteps: Set<number>;
    currentStep: number;
    handleGoToStep: (step: number) => void;
    handleNext: () => void;
    handlePrevious: () => void;
}

export default function useDataStreamEditor(): UseDataStreamEditorResultI {
    const [currentStep, setCurrentStep] = useState(0);

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const configuredSteps = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return new Set<number>();
        }

        const steps = new Set<number>();

        if (clusterElements['source']) {
            steps.add(0);
        }

        if (clusterElements['destination']) {
            steps.add(1);
        }

        if (clusterElements['processor']) {
            steps.add(2);
        }

        return steps;
    }, [rootClusterElementNodeData?.clusterElements]);

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
        configuredSteps,
        currentStep,
        handleGoToStep,
        handleNext,
        handlePrevious,
    };
}
