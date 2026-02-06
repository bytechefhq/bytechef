import ApiConnectorWizardImportStep from '../components/wizard/ApiConnectorWizardImportStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorImportPage from './hooks/useApiConnectorImportPage';

const ApiConnectorImportPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isPending, previousStep} =
        useApiConnectorImportPage();

    const renderStepContent = () => {
        if (currentStep === 0) {
            return <ApiConnectorWizardImportStep />;
        }

        return <ApiConnectorWizardReviewStep mode="import" />;
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
            pageTitle="Import Open API"
            steps={WIZARD_STEPS.import}
        >
            {renderStepContent()}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorImportPage;
