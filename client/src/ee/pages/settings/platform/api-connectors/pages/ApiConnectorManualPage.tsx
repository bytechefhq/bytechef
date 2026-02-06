import ApiConnectorWizardBasicStep from '../components/wizard/ApiConnectorWizardBasicStep';
import ApiConnectorWizardEndpointsStep from '../components/wizard/ApiConnectorWizardEndpointsStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorManualPage from './hooks/useApiConnectorManualPage';

const ApiConnectorManualPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isPending, previousStep} =
        useApiConnectorManualPage();

    const renderStepContent = () => {
        if (currentStep === 0) {
            return <ApiConnectorWizardBasicStep />;
        }

        if (currentStep === 1) {
            return <ApiConnectorWizardEndpointsStep />;
        }

        return <ApiConnectorWizardReviewStep mode="manual" />;
    };

    return (
        <ApiConnectorWizardLayout
            canProceed={canProceed}
            currentStep={currentStep}
            isPending={isPending}
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
