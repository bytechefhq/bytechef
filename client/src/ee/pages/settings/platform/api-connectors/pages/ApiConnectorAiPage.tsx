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
            {currentStep === 0 && isProcessing && (
                <div className="flex flex-col items-center justify-center py-12">
                    <LoadingIcon className="size-8" />

                    <p className="mt-4 text-sm text-gray-600">Generating OpenAPI specification...</p>

                    <p className="mt-2 text-xs text-gray-500">This may take a minute</p>
                </div>
            )}

            {currentStep === 0 && !isProcessing && <ApiConnectorWizardDocUrlStep />}

            {currentStep === 1 && <ApiConnectorWizardEndpointSelectionStep />}

            {currentStep >= 2 && <ApiConnectorWizardReviewStep mode="ai" />}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorAiPage;
