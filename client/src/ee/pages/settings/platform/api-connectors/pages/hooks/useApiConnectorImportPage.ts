import {useEffect} from 'react';
import {toast} from 'sonner';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import {hydrateWizardEndpointsFromSpecification} from '../../utils/specification-utils';
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
    const {
        currentStep,
        endpoints,
        name,
        nextStep,
        previousStep,
        reset,
        setBaseSpecification,
        setBaseUrl,
        setEndpoints,
        specification,
    } = useApiConnectorWizardStore();

    const {handleCancel, handleSave, isPending} = useImportApiConnector();

    const handleNext = () => {
        if (currentStep === 0) {
            const hydrated = hydrateWizardEndpointsFromSpecification(specification || '', {
                setBaseSpecification,
                setBaseUrl,
                setEndpoints,
            });

            if (!hydrated) {
                toast.error('Failed to parse specification', {
                    description: 'Please check that the imported file is valid YAML/OpenAPI format.',
                });

                return;
            }
        }

        nextStep();
    };

    const canProceed = (() => {
        if (currentStep === 0) {
            return !!name && !!specification;
        }

        if (currentStep === 1) {
            return endpoints.length > 0;
        }

        return true;
    })();

    useEffect(() => {
        reset();
    }, [reset]);

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
