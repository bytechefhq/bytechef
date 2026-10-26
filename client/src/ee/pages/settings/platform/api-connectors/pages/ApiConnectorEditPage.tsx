import LoadingIcon from '@/components/LoadingIcon';

import ApiConnectorWizardBasicStep from '../components/wizard/ApiConnectorWizardBasicStep';
import ApiConnectorWizardEndpointsStep from '../components/wizard/ApiConnectorWizardEndpointsStep';
import ApiConnectorWizardLayout from '../components/wizard/ApiConnectorWizardLayout';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';
import useApiConnectorEditPage from './hooks/useApiConnectorEditPage';

const ApiConnectorEditPage = () => {
    const {canProceed, currentStep, handleCancel, handleNext, handleSave, isHydrated, isPending, previousStep} =
        useApiConnectorEditPage();

    if (!isHydrated) {
        return (
            <div className="flex size-full items-center justify-center">
                <LoadingIcon /> Loading API Connector...
            </div>
        );
    }

    return (
        <ApiConnectorWizardLayout
            canProceed={canProceed}
            currentStep={currentStep}
            isPending={isPending}
            onCancel={handleCancel}
            onNext={handleNext}
            onPrevious={previousStep}
            onSave={handleSave}
            pageTitle="Edit API Connector"
            steps={WIZARD_STEPS.manual}
        >
            {currentStep === 0 && <ApiConnectorWizardBasicStep />}

            {currentStep === 1 && <ApiConnectorWizardEndpointsStep />}

            {currentStep >= 2 && <ApiConnectorWizardReviewStep />}
        </ApiConnectorWizardLayout>
    );
};

export default ApiConnectorEditPage;
