import {twMerge} from 'tailwind-merge';

import DataStreamDestinationStep from './components/DataStreamDestinationStep';
import DataStreamHeader from './components/DataStreamHeader';
import DataStreamMappingStep from './components/DataStreamMappingStep';
import DataStreamSourceStep from './components/DataStreamSourceStep';
import DataStreamStepNav from './components/DataStreamStepNav';
import DataStreamWizardFooter from './components/DataStreamWizardFooter';
import useDataStreamDataPills from './hooks/useDataStreamDataPills';
import useDataStreamEditor from './hooks/useDataStreamEditor';

const STEP_LABELS = ['Source', 'Destination', 'Mapping'];

interface DataStreamEditorProps {
    className?: string;
    onClose?: () => void;
    onToggleEditor?: (showDataStream: boolean) => void;
}

export default function DataStreamEditor({className, onClose, onToggleEditor}: DataStreamEditorProps) {
    const {configuredSteps, currentStep, handleGoToStep, handleNext, handlePrevious} = useDataStreamEditor();

    useDataStreamDataPills();

    function renderCurrentStep() {
        switch (currentStep) {
            case 0:
                return <DataStreamSourceStep />;
            case 1:
                return <DataStreamDestinationStep />;
            case 2:
                return <DataStreamMappingStep />;
            default:
                return null;
        }
    }

    return (
        <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-white', className)}>
            <DataStreamHeader onClose={onClose} onToggleEditor={onToggleEditor} />

            <DataStreamStepNav
                configuredSteps={configuredSteps}
                currentStep={currentStep}
                onGoToStep={handleGoToStep}
                stepLabels={STEP_LABELS}
            />

            <div className="min-h-0 flex-1 overflow-y-auto">
                <div className="mx-auto w-full max-w-2xl px-4">{renderCurrentStep()}</div>
            </div>

            <DataStreamWizardFooter currentStep={currentStep} onNext={handleNext} onPrevious={handlePrevious} />
        </div>
    );
}
