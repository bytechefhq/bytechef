import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {NodeDataType} from '@/shared/types';
import {useMemo} from 'react';
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
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const {currentStep, handleGoToStep, handleNext, handlePrevious} = useDataStreamEditor();

    useDataStreamDataPills();

    const isSimpleModeAvailable = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return true;
        }

        const processorValue = clusterElements['processor'];

        if (!processorValue) {
            return true;
        }

        const processorElement = (Array.isArray(processorValue)
            ? processorValue[0]
            : processorValue) as unknown as NodeDataType;

        const typeSegments = processorElement.type?.split('/') || [];
        const componentName = processorElement.componentName || typeSegments[0] || '';
        const clusterElementName = typeSegments[2] || '';

        return componentName === 'dataStreamProcessor' && clusterElementName === 'fieldMapper';
    }, [rootClusterElementNodeData?.clusterElements]);

    const configuredSteps = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return new Set<number>();
        }

        const steps = new Set<number>();

        if (clusterElements['source']) {
            steps.add(0);
        }

        if (clusterElements['destination']) {
            steps.add(1);
        }

        if (clusterElements['processor']) {
            steps.add(2);
        }

        return steps;
    }, [rootClusterElementNodeData?.clusterElements]);

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
            <DataStreamHeader
                isSimpleModeAvailable={isSimpleModeAvailable}
                onClose={onClose}
                onToggleEditor={onToggleEditor}
            />

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
