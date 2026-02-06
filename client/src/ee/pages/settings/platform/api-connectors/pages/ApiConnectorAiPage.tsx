import LoadingIcon from '@/components/LoadingIcon';

import ApiConnectorWizardDocUrlStep from '../components/wizard/ApiConnectorWizardDocUrlStep';
import ApiConnectorWizardEndpointSelectionStep from '../components/wizard/ApiConnectorWizardEndpointSelectionStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorAiPage from './hooks/useApiConnectorAiPage';

const ApiConnectorAiPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isPending, isProcessing, previousStep} =
        useApiConnectorAiPage();

    const renderStepContent = () => {
        if (currentStep === 0 && isProcessing) {
            return (
                <div className="flex flex-col items-center justify-center py-12">
                    <LoadingIcon className="size-8" />

                    <p className="mt-4 text-sm text-gray-600">Generating OpenAPI specification...</p>

                    <p className="mt-2 text-xs text-gray-500">This may take a minute</p>
                </div>
            );
        }

        if (currentStep === 0) {
            return <ApiConnectorWizardDocUrlStep />;
        }

        if (currentStep === 1) {
            return <ApiConnectorWizardEndpointSelectionStep />;
        }

        return <ApiConnectorWizardReviewStep mode="ai" />;
    };

    return (
        <ApiConnectorWizardLayout
            canProceed={canProceed}
            currentStep={currentStep}
            isPending={isPending}
            isProcessing={isProcessing}
            onCancel={handleCancel}
            onNext={handleNext}
            onPrevious={previousStep}
            onSave={handleSave}
            pageTitle="Create from Documentation"
            primaryButtonLabel={currentStep === 0 ? 'Generate' : 'Next'}
            steps={WIZARD_STEPS.ai}
        >
            {renderStepContent()}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorAiPage;
