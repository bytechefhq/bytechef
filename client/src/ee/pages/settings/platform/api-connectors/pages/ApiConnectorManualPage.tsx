import {useImportOpenApiSpecificationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';

import ApiConnectorWizardBasicStep from '../components/wizard/ApiConnectorWizardBasicStep';
import ApiConnectorWizardEndpointsStep from '../components/wizard/ApiConnectorWizardEndpointsStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {useApiConnectorWizardStore} from '../stores/useApiConnectorWizardStore';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';

const ApiConnectorManualPage = () => {
    const {currentStep, endpoints, icon, name, nextStep, previousStep, reset, specification} =
        useApiConnectorWizardStore();

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

    const renderStepContent = () => {
        if (currentStep === 0) {
            return <ApiConnectorWizardBasicStep />;
        }

        if (currentStep === 1) {
            return <ApiConnectorWizardEndpointsStep />;
        }

        return <ApiConnectorWizardReviewStep mode="manual" />;
    };

    const canProceed = () => {
        if (currentStep === 0) {
            return !!name;
        }

        if (currentStep === 1) {
            return endpoints.length > 0;
        }

        return true;
    };

    return (
        <ApiConnectorWizardLayout
            canProceed={canProceed()}
            currentStep={currentStep}
            isPending={importOpenApiSpecificationMutation.isPending}
            onCancel={handleCancel}
            onNext={handleNext}
            onPrevious={previousStep}
            onSave={handleSave}
            pageTitle="Create API Connector"
            steps={WIZARD_STEPS.manual}
        >
            {renderStepContent()}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorManualPage;
