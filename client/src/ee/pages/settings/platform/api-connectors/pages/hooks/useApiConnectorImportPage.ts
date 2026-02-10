import {useEffect} from 'react';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import useImportApiConnector from './useImportApiConnector';

interface UseApiConnectorImportPageI {
    canProceed: boolean;
    currentStep: number;
    handleCancel: () => void;
    handleNext: () => void;
    handleSave: () => void;
    isPending: boolean;
    previousStep: () => void;
}

const useApiConnectorImportPage = (): UseApiConnectorImportPageI => {
    const {currentStep, name, nextStep, previousStep, reset, specification} = useApiConnectorWizardStore();

    const {handleCancel, handleSave, isPending} = useImportApiConnector();

    useEffect(() => {
        reset();
    }, [reset]);

    const handleNext = () => {
        nextStep();
    };

    const canProceed = (() => {
        if (currentStep === 0) {
            return !!name && !!specification;
        }

        return true;
    })();

    return {
        canProceed,
        currentStep,
        handleCancel,
        handleNext,
        handleSave,
        isPending,
        previousStep,
    };
};

export default useApiConnectorImportPage;
