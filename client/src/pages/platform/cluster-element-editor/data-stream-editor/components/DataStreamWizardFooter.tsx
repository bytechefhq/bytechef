import Button from '@/components/Button/Button';
import {ChevronLeftIcon, ChevronRightIcon} from 'lucide-react';

const TOTAL_STEPS = 3;

interface DataStreamWizardFooterProps {
    currentStep: number;
    onNext: () => void;
    onPrevious: () => void;
}

export default function DataStreamWizardFooter({currentStep, onNext, onPrevious}: DataStreamWizardFooterProps) {
    const isFirstStep = currentStep === 0;
    const isLastStep = currentStep === TOTAL_STEPS - 1;

    return (
        <div className="flex items-center justify-end border-t px-4 py-3">
            <div className="flex items-center gap-2">
                <Button
                    disabled={isFirstStep}
                    icon={<ChevronLeftIcon />}
                    label="Previous"
                    onClick={onPrevious}
                    size="default"
                    variant="outline"
                />

                <Button disabled={isLastStep} onClick={onNext} size="default" variant="default">
                    <span>{isLastStep ? 'Finish' : 'Next'}</span>

                    <ChevronRightIcon />
                </Button>
            </div>
        </div>
    );
}
