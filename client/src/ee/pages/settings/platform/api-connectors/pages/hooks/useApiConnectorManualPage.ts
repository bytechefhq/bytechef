import {useEffect} from 'react';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import useImportApiConnector from './useImportApiConnector';

interface UseApiConnectorManualPageI {
    canProceed: boolean;
    currentStep: number;
    handleCancel: () => void;
    handleNext: () => void;
    handleSave: () => void;
    isPending: boolean;
    previousStep: () => void;
}

const useApiConnectorManualPage = (): UseApiConnectorManualPageI => {
    const {currentStep, endpoints, name, nextStep, previousStep, reset} = useApiConnectorWizardStore();

    const {handleCancel, handleSave, isPending} = useImportApiConnector();

    useEffect(() => {
        reset();
    }, [reset]);

    const handleNext = () => {
        nextStep();
    };

    const canProceed = (() => {
        if (currentStep === 0) {
            return !!name;
        }

        return endpoints.length > 0;
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

export default useApiConnectorManualPage;
