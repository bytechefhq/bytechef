import DataSyncElementStep from '@/pages/automation/data-syncs/components/wizard/DataSyncElementStep';
import DataSyncMappingStep from '@/pages/automation/data-syncs/components/wizard/DataSyncMappingStep';
import DataSyncStepNav from '@/pages/automation/data-syncs/components/wizard/DataSyncStepNav';
import DataSyncTestStep from '@/pages/automation/data-syncs/components/wizard/DataSyncTestStep';
import DataSyncTriggerStep from '@/pages/automation/data-syncs/components/wizard/DataSyncTriggerStep';
import DataSyncWizardFooter from '@/pages/automation/data-syncs/components/wizard/DataSyncWizardFooter';
import {findElement} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import {STEP_LABELS, TOTAL_STEPS} from '@/pages/automation/data-syncs/utils/dataSyncWizardSteps';
import {DataSync, DataSyncElementKind} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface DataSyncWizardProps {
    dataSync: DataSync;
}

export default function DataSyncWizard({dataSync}: DataSyncWizardProps) {
    const [currentStep, setCurrentStep] = useState(0);

    const navigate = useNavigate();

    const configuredSteps = useMemo(() => {
        const steps = new Set<number>([0]);

        if (findElement(dataSync, DataSyncElementKind.Source)) {
            steps.add(1);
        }

        if (findElement(dataSync, DataSyncElementKind.Destination)) {
            steps.add(2);
        }

        if (findElement(dataSync, DataSyncElementKind.Processor)) {
            steps.add(3);
        }

        return steps;
    }, [dataSync]);

    const handleGoToStep = useCallback((step: number) => setCurrentStep(step), []);
    const handleNext = useCallback(() => setCurrentStep((step) => Math.min(step + 1, TOTAL_STEPS - 1)), []);
    const handlePrevious = useCallback(() => setCurrentStep((step) => Math.max(step - 1, 0)), []);

    function renderCurrentStep() {
        switch (currentStep) {
            case 0:
                return <DataSyncTriggerStep dataSync={dataSync} key={dataSync.id} />;
            case 1:
                return (
                    <DataSyncElementStep
                        dataSync={dataSync}
                        key={DataSyncElementKind.Source}
                        kind={DataSyncElementKind.Source}
                    />
                );
            case 2:
                return (
                    <DataSyncElementStep
                        dataSync={dataSync}
                        key={DataSyncElementKind.Destination}
                        kind={DataSyncElementKind.Destination}
                    />
                );
            case 3:
                return <DataSyncMappingStep dataSync={dataSync} />;
            case 4:
                return <DataSyncTestStep draftWorkflowId={dataSync.draftWorkflowId} />;
            default:
                return null;
        }
    }

    return (
        <div className="flex h-full flex-1 flex-col bg-surface-neutral-primary">
            <DataSyncStepNav
                configuredSteps={configuredSteps}
                currentStep={currentStep}
                onGoToStep={handleGoToStep}
                stepLabels={STEP_LABELS}
            />

            <div className="min-h-0 flex-1 overflow-y-auto">
                <div className="mx-auto w-full max-w-2xl px-4">{renderCurrentStep()}</div>
            </div>

            <DataSyncWizardFooter
                currentStep={currentStep}
                onFinish={() => navigate('/automation/projects?dataSyncs=all')}
                onNext={handleNext}
                onPrevious={handlePrevious}
            />
        </div>
    );
}
