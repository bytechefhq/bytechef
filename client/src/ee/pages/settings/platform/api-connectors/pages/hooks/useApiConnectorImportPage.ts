import {useImportOpenApiSpecificationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';

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
    const {currentStep, icon, name, nextStep, previousStep, reset, specification} = useApiConnectorWizardStore();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    useEffect(() => {
        reset();
    }, [reset]);

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['apiConnectors'],
        });

        reset();
        navigate('/automation/settings/api-connectors');
    };

    const importOpenApiSpecificationMutation = useImportOpenApiSpecificationMutation({onSuccess});

    const handleNext = () => {
        nextStep();
    };

    const handleSave = () => {
        if (!name || !specification) {
            return;
        }

        importOpenApiSpecificationMutation.mutate({
            input: {
                icon: icon || undefined,
                name,
                specification,
            },
        });
    };

    const handleCancel = () => {
        reset();
        navigate('/automation/settings/api-connectors');
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
        isPending: importOpenApiSpecificationMutation.isPending,
        previousStep,
    };
};

export default useApiConnectorImportPage;
