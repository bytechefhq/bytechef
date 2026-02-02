import {useImportOpenApiSpecificationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';

import ApiConnectorWizardImportStep from '../components/wizard/ApiConnectorWizardImportStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {useApiConnectorWizardStore} from '../stores/useApiConnectorWizardStore';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';

const ApiConnectorImportPage = () => {
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

    const renderStepContent = () => {
        if (currentStep === 0) {
            return <ApiConnectorWizardImportStep />;
        }

        return <ApiConnectorWizardReviewStep mode="import" />;
    };

    const canProceed = () => {
        if (currentStep === 0) {
            return !!name && !!specification;
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
            pageTitle="Import Open API"
            steps={WIZARD_STEPS.import}
        >
            {renderStepContent()}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorImportPage;
