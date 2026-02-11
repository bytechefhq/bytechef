import ApiConnectorWizardBasicStep from '../components/wizard/ApiConnectorWizardBasicStep';
import ApiConnectorWizardEndpointsStep from '../components/wizard/ApiConnectorWizardEndpointsStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorManualPage from './hooks/useApiConnectorManualPage';

const ApiConnectorManualPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isPending, previousStep} =
        useApiConnectorManualPage();

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
            {currentStep === 0 && <ApiConnectorWizardBasicStep />}

            {currentStep === 1 && <ApiConnectorWizardEndpointsStep />}

            {currentStep >= 2 && <ApiConnectorWizardReviewStep mode="manual" />}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorManualPage;
