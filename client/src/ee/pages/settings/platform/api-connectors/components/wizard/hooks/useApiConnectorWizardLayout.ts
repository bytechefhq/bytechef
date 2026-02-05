import {useMemo} from 'react';

interface UseApiConnectorWizardLayoutProps {
    currentStep: number;
    isProcessing?: boolean;
    pageTitle: string;
    primaryButtonLabel?: string;
    steps: readonly string[];
}

interface UseApiConnectorWizardLayoutI {
    cancelButtonLabel: string;
    isFirstStep: boolean;
    isLastStep: boolean;
    pageTitleWithStep: string;
    primaryLabel: string;
    stepCount: number;
}

export default function useApiConnectorWizardLayout({
    currentStep,
    isProcessing = false,
    pageTitle,
    primaryButtonLabel,
    steps,
}: UseApiConnectorWizardLayoutProps): UseApiConnectorWizardLayoutI {
    const stepCount = steps.length;
    const isLastStep = currentStep === stepCount - 1;
    const isFirstStep = currentStep === 0;

    const pageTitleWithStep = useMemo(() => {
        const stepName = steps[currentStep] || '';

        return `${pageTitle} - ${stepName}`;
    }, [currentStep, pageTitle, steps]);

    const cancelButtonLabel = useMemo(() => {
        if (isProcessing) {
            return 'Cancel Generation';
        }

        return 'Cancel';
    }, [isProcessing]);

    const primaryLabel = useMemo(() => {
        if (isLastStep) {
            return 'Save';
        }

        return primaryButtonLabel || 'Next';
    }, [isLastStep, primaryButtonLabel]);

    return {
        cancelButtonLabel,
        isFirstStep,
        isLastStep,
        pageTitleWithStep,
        primaryLabel,
        stepCount,
    };
}
