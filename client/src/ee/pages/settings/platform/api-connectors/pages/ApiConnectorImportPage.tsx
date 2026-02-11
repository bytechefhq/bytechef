import ApiConnectorWizardImportStep from '../components/wizard/ApiConnectorWizardImportStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorImportPage from './hooks/useApiConnectorImportPage';

const ApiConnectorImportPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isPending, previousStep} =
        useApiConnectorImportPage();

    return (
        <ApiConnectorWizardLayout
            canProceed={canProceed}
            currentStep={currentStep}
            isPending={isPending}
            onCancel={handleCancel}
            onNext={handleNext}
            onPrevious={previousStep}
            onSave={handleSave}
            pageTitle="Import OpenAPI"
            steps={WIZARD_STEPS.import}
        >
            {currentStep === 0 ? <ApiConnectorWizardImportStep /> : <ApiConnectorWizardReviewStep mode="import" />}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorImportPage;
