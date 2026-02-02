import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {useImportOpenApiSpecificationMutation} from '@/shared/middleware/graphql';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import ApiConnectorWizardImportStep from '../components/wizard/ApiConnectorWizardImportStep';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {useApiConnectorWizardStore} from '../stores/useApiConnectorWizardStore';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';

const ApiConnectorImportPage = () => {
    const {currentStep, icon, name, nextStep, previousStep, reset, setCurrentStep, specification} =
        useApiConnectorWizardStore();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    useEffect(() => {
        setCurrentStep(0);
    }, [setCurrentStep]);

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['apiConnectors'],
        });

        reset();
        navigate('/automation/settings/api-connectors');
    };

    const importOpenApiSpecificationMutation = useImportOpenApiSpecificationMutation({onSuccess});

    const stepCount = WIZARD_STEPS.import.length;
    const isLastStep = currentStep === stepCount - 1;
    const isFirstStep = currentStep === 0;

    const handleNext = () => {
        nextStep();
    };

    const handleSave = () => {
        if (!name || !specification) {
            return;
        }

        importOpenApiSpecificationMutation.mutate({
            input: {
                icon: icon || undefined,
                name,
                specification,
            },
        });
    };

    const handleCancel = () => {
        reset();
        navigate('/automation/settings/api-connectors');
    };

    const renderStepContent = () => {
        if (currentStep === 0) {
            return <ApiConnectorWizardImportStep />;
        }

        return <ApiConnectorWizardReviewStep mode="import" />;
    };

    const canProceed = () => {
        if (currentStep === 0) {
            return !!name && !!specification;
        }

        return true;
    };

    const getPageTitle = () => {
        const stepName = WIZARD_STEPS.import[currentStep] || '';

        return `Import Open API - ${stepName}`;
    };

    const isPending = importOpenApiSpecificationMutation.isPending;

    return (
        <div className="flex min-h-full items-center justify-center p-6">
            <div className="w-full max-w-2xl">
                <div className="rounded-lg border bg-white shadow-sm">
                    <div className="flex flex-col gap-1 border-b p-6">
                        <div className="flex items-center justify-between">
                            <h1 className="text-lg font-semibold">{getPageTitle()}</h1>

                            <button
                                className="inline-flex size-9 items-center justify-center rounded-md text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                                onClick={handleCancel}
                                type="button"
                            >
                                <Cross2Icon className="size-4" />

                                <span className="sr-only">Close</span>
                            </button>
                        </div>

                        <nav aria-label="Progress">
                            <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                {WIZARD_STEPS.import.map((stepLabel, index) => (
                                    <li className="md:flex-1" key={stepLabel}>
                                        <div
                                            className={twMerge(
                                                'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0',
                                                index <= currentStep
                                                    ? 'border-gray-900 hover:border-gray-800'
                                                    : 'border-gray-200 hover:border-gray-300'
                                            )}
                                        />
                                    </li>
                                ))}
                            </ol>
                        </nav>
                    </div>

                    <div className="max-h-[60vh] overflow-y-auto p-6">{renderStepContent()}</div>

                    <div className="flex justify-between border-t p-6">
                        {isFirstStep ? (
                            <Button onClick={handleCancel} variant="outline">
                                <ArrowLeftIcon className="mr-2 size-4" />
                                Cancel
                            </Button>
                        ) : (
                            <Button onClick={previousStep} variant="outline">
                                <ArrowLeftIcon className="mr-2 size-4" />
                                Previous
                            </Button>
                        )}

                        {isLastStep ? (
                            <Button
                                disabled={isPending || !canProceed()}
                                icon={isPending ? <LoadingIcon /> : undefined}
                                onClick={handleSave}
                            >
                                Save
                            </Button>
                        ) : (
                            <Button disabled={!canProceed()} onClick={handleNext}>
                                Next
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ApiConnectorImportPage;
