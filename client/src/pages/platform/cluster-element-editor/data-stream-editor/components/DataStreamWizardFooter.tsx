import Button from '@/components/Button/Button';
import {TOTAL_STEPS} from '@/pages/platform/cluster-element-editor/data-stream-editor/hooks/useDataStreamEditor';
import {CheckIcon, ChevronLeftIcon, ChevronRightIcon} from 'lucide-react';

interface DataStreamWizardFooterProps {
    currentStep: number;
    onFinish?: () => void;
    onNext: () => void;
    onPrevious: () => void;
}

export default function DataStreamWizardFooter({
    currentStep,
    onFinish,
    onNext,
    onPrevious,
}: DataStreamWizardFooterProps) {
    const isFirstStep = currentStep === 0;
    const isLastStep = currentStep === TOTAL_STEPS - 1;

    return (
        <div className="flex items-center justify-end border-t border-t-border/50 px-4 py-3">
            <div className="flex items-center gap-2">
                <Button
                    disabled={isFirstStep}
                    icon={<ChevronLeftIcon />}
                    label="Previous"
                    onClick={onPrevious}
                    size="default"
                    variant="outline"
                />

                {isLastStep ? (
                    <Button icon={<CheckIcon />} label="Finish" onClick={onFinish} size="default" variant="default" />
                ) : (
                    <Button onClick={onNext} size="default" variant="default">
                        <span>Next</span>

                        <ChevronRightIcon />
                    </Button>
                )}
            </div>
        </div>
    );
}
