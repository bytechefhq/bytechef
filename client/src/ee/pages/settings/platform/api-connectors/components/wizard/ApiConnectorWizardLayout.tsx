import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ArrowLeftIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import useApiConnectorWizardLayout from './hooks/useApiConnectorWizardLayout';

interface ApiConnectorWizardLayoutProps {
    canProceed: boolean;
    children: ReactNode;
    currentStep: number;
    isPending?: boolean;
    isProcessing?: boolean;
    onCancel: () => void;
    onNext: () => void;
    onPrevious: () => void;
    onSave: () => void;
    pageTitle: string;
    primaryButtonLabel?: string;
    steps: readonly string[];
}

const ApiConnectorWizardLayout = ({
    canProceed,
    children,
    currentStep,
    isPending = false,
    isProcessing = false,
    onCancel,
    onNext,
    onPrevious,
    onSave,
    pageTitle,
    primaryButtonLabel,
    steps,
}: ApiConnectorWizardLayoutProps) => {
    const {cancelButtonLabel, isFirstStep, isLastStep, pageTitleWithStep, primaryLabel} = useApiConnectorWizardLayout({
        currentStep,
        isProcessing,
        pageTitle,
        primaryButtonLabel,
        steps,
    });

    return (
        <div className="flex min-h-full items-center justify-center p-6">
            <div className="w-full max-w-2xl">
                <div className="rounded-lg border bg-white shadow-sm">
                    <div className="flex flex-col gap-1 border-b p-6">
                        <div className="flex items-center justify-between">
                            <h1 className="text-lg font-semibold">{pageTitleWithStep}</h1>

                            <Button
                                aria-label="Close"
                                icon={<Cross2Icon className="size-4" />}
                                onClick={onCancel}
                                size="icon"
                                variant="ghost"
                            />
                        </div>

                        <nav aria-label="Progress">
                            <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                {steps.map((step, index) => (
                                    <li
                                        className={twMerge(
                                            'group flex flex-col border-l-4 py-2 pl-4 md:flex-1 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-2',
                                            index <= currentStep
                                                ? 'border-gray-900 hover:border-gray-800'
                                                : 'border-gray-200 hover:border-gray-300'
                                        )}
                                        key={step}
                                    >
                                        <span
                                            className={twMerge(
                                                'text-xs font-medium',
                                                index <= currentStep ? 'text-gray-900' : 'text-gray-500'
                                            )}
                                        >
                                            {step}
                                        </span>
                                    </li>
                                ))}
                            </ol>
                        </nav>
                    </div>

                    <div className="max-h-[60vh] overflow-y-auto p-6">{children}</div>

                    <div className="flex justify-between border-t p-6">
                        {isFirstStep ? (
                            <Button disabled={isProcessing} onClick={onCancel} variant="outline">
                                <ArrowLeftIcon className="mr-2 size-4" />

                                {cancelButtonLabel}
                            </Button>
                        ) : (
                            <Button onClick={onPrevious} variant="outline">
                                <ArrowLeftIcon className="mr-2 size-4" />
                                Previous
                            </Button>
                        )}

                        {isLastStep ? (
                            <Button
                                disabled={isPending || !canProceed}
                                icon={isPending ? <LoadingIcon /> : undefined}
                                onClick={onSave}
                            >
                                {primaryLabel}
                            </Button>
                        ) : (
                            <Button
                                disabled={!canProceed || isProcessing || isPending}
                                icon={isPending || isProcessing ? <LoadingIcon /> : undefined}
                                onClick={onNext}
                            >
                                {primaryLabel}
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ApiConnectorWizardLayout;
