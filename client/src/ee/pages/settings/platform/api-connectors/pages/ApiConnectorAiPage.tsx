import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {
    useCancelGenerationJobMutation,
    useGenerationJobStatusQuery,
    useImportOpenApiSpecificationMutation,
    useStartGenerateFromDocumentationPreviewMutation,
} from '@/shared/middleware/graphql';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import ApiConnectorWizardDocUrlStep from '../components/wizard/ApiConnectorWizardDocUrlStep';
import ApiConnectorWizardReviewStep from '../components/wizard/ApiConnectorWizardReviewStep';
import {useApiConnectorWizardStore} from '../stores/useApiConnectorWizardStore';
import {WIZARD_STEPS} from '../types/api-connector-wizard.types';

const ApiConnectorAiPage = () => {
    const {
        currentStep,
        documentationUrl,
        icon,
        isProcessing,
        jobId,
        name,
        nextStep,
        previousStep,
        reset,
        setCurrentStep,
        setError,
        setIsProcessing,
        setJobId,
        setSpecification,
        specification,
        userPrompt,
    } = useApiConnectorWizardStore();

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

    const startGenerationMutation = useStartGenerateFromDocumentationPreviewMutation({
        onError: (error: Error) => {
            setError(error.message);
            setIsProcessing(false);
        },
        onSuccess: (result) => {
            if (result.startGenerateFromDocumentationPreview.jobId) {
                setJobId(result.startGenerateFromDocumentationPreview.jobId);
            }
        },
    });

    const cancelGenerationMutation = useCancelGenerationJobMutation();

    const {data: jobStatusData} = useGenerationJobStatusQuery(
        {jobId: jobId || ''},
        {
            enabled: !!jobId,
            refetchInterval: (query) => {
                const status = query.state.data?.generationJobStatus?.status;

                if (status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED') {
                    return false;
                }

                return 1000;
            },
        }
    );

    const jobStatus = jobStatusData?.generationJobStatus;

    useEffect(() => {
        if (!jobStatus) {
            return;
        }

        if (jobStatus.status === 'COMPLETED' && jobStatus.specification) {
            setSpecification(jobStatus.specification);
            setIsProcessing(false);
            setJobId(null);
            nextStep();
        } else if (jobStatus.status === 'FAILED') {
            setError(jobStatus.errorMessage || 'Generation failed');
            setIsProcessing(false);
            setJobId(null);
        } else if (jobStatus.status === 'CANCELLED') {
            setIsProcessing(false);
            setJobId(null);
        }
    }, [jobStatus, nextStep, setError, setIsProcessing, setJobId, setSpecification]);

    const stepCount = WIZARD_STEPS.ai.length;
    const isLastStep = currentStep === stepCount - 1;
    const isFirstStep = currentStep === 0;

    const handleNext = () => {
        if (currentStep === 0) {
            if (!name || !documentationUrl) {
                return;
            }

            setIsProcessing(true);
            setError(undefined);

            startGenerationMutation.mutate({
                input: {
                    documentationUrl,
                    icon: icon || undefined,
                    name,
                    userPrompt: userPrompt || undefined,
                },
            });
        } else {
            nextStep();
        }
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
        if (jobId && isProcessing) {
            cancelGenerationMutation.mutate({jobId});
        }

        reset();
        navigate('/automation/settings/api-connectors');
    };

    const renderStepContent = () => {
        if (currentStep === 0 && isProcessing) {
            return (
                <div className="flex flex-col items-center justify-center py-12">
                    <LoadingIcon className="size-8" />

                    <p className="mt-4 text-sm text-gray-600">Generating OpenAPI specification...</p>

                    <p className="mt-2 text-xs text-gray-500">This may take a minute</p>
                </div>
            );
        }

        if (currentStep === 0) {
            return <ApiConnectorWizardDocUrlStep />;
        }

        return <ApiConnectorWizardReviewStep mode="ai" />;
    };

    const canProceed = () => {
        if (currentStep === 0) {
            return !!name && !!documentationUrl;
        }

        return true;
    };

    const getPageTitle = () => {
        const stepName = WIZARD_STEPS.ai[currentStep] || '';

        return `Create from Documentation - ${stepName}`;
    };

    const isPending = importOpenApiSpecificationMutation.isPending || startGenerationMutation.isPending;

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
                                {WIZARD_STEPS.ai.map((stepLabel, index) => (
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
                            <Button disabled={isProcessing && !!jobId} onClick={handleCancel} variant="outline">
                                <ArrowLeftIcon className="mr-2 size-4" />

                                {isProcessing && jobId ? 'Cancel Generation' : 'Cancel'}
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
                            <Button
                                disabled={!canProceed() || isProcessing || isPending}
                                icon={isPending || isProcessing ? <LoadingIcon /> : undefined}
                                onClick={handleNext}
                            >
                                {currentStep === 0 ? 'Generate' : 'Next'}
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ApiConnectorAiPage;
