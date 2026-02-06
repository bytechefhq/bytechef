import {toast} from '@/hooks/use-toast';
import {
    HttpMethod,
    useCancelGenerationJobMutation,
    useGenerationJobStatusQuery,
    useImportOpenApiSpecificationMutation,
    useStartGenerateFromDocumentationPreviewMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useRef} from 'react';
import {useNavigate} from 'react-router-dom';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import {DiscoveredEndpointI} from '../../types/api-connector-wizard.types';

// Maximum number of polling attempts before timing out (5 minutes at 1 second intervals)
const MAX_POLLING_ATTEMPTS = 300;

interface UseApiConnectorAiPageI {
    canProceed: boolean;
    currentStep: number;
    handleCancel: () => Promise<void>;
    handleNext: () => void;
    handleSave: () => void;
    isPending: boolean;
    isProcessing: boolean;
    previousStep: () => void;
}

const useApiConnectorAiPage = (): UseApiConnectorAiPageI => {
    const {
        currentStep,
        discoveredEndpoints,
        documentationUrl,
        icon,
        isProcessing,
        jobId,
        name,
        nextStep,
        previousStep,
        reset,
        selectAllEndpoints,
        selectedEndpointIds,
        setDiscoveredEndpoints,
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
        reset();
    }, [reset]);

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

    const pollingStartTimeRef = useRef<number | null>(null);

    const {data: jobStatusData} = useGenerationJobStatusQuery(
        {jobId: jobId || ''},
        {
            enabled: !!jobId,
            refetchInterval: (query) => {
                const status = query.state.data?.generationJobStatus?.status;

                if (status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED') {
                    pollingStartTimeRef.current = null;

                    return false;
                }

                // Initialize start time on first poll
                if (pollingStartTimeRef.current === null) {
                    pollingStartTimeRef.current = Date.now();
                }

                // Check if we've exceeded the maximum polling duration (5 minutes)
                const elapsedMs = Date.now() - pollingStartTimeRef.current;

                if (elapsedMs >= MAX_POLLING_ATTEMPTS * 1000) {
                    pollingStartTimeRef.current = null;

                    return false;
                }

                return 1000;
            },
        }
    );

    const jobStatus = jobStatusData?.generationJobStatus;

    // Track if job completed to prevent timeout from firing after completion
    const jobCompletedRef = useRef(false);

    // Handle polling timeout by checking elapsed time
    useEffect(() => {
        if (!jobId || !isProcessing) {
            jobCompletedRef.current = false;

            return;
        }

        const timeoutId = setTimeout(() => {
            // Don't show timeout error if job already completed
            if (jobCompletedRef.current) {
                return;
            }

            if (isProcessing && jobId) {
                setError('Generation timed out after 5 minutes. Please try again.');
                setIsProcessing(false);
                setJobId(null);
                pollingStartTimeRef.current = null;
            }
        }, MAX_POLLING_ATTEMPTS * 1000);

        return () => clearTimeout(timeoutId);
    }, [isProcessing, jobId, setError, setIsProcessing, setJobId]);

    const parseEndpointsFromSpecification = useCallback((spec: string): DiscoveredEndpointI[] => {
        try {
            const parsed = yamlParse(spec);
            const endpoints: DiscoveredEndpointI[] = [];

            if (parsed.paths) {
                Object.entries(parsed.paths).forEach(([path, methods]) => {
                    Object.entries(methods as Record<string, Record<string, unknown>>).forEach(
                        ([method, operation]) => {
                            const httpMethod = method.toUpperCase();

                            if (Object.values(HttpMethod).includes(httpMethod as HttpMethod)) {
                                const tags = operation.tags as string[] | undefined;
                                const resource = tags?.[0] || path.split('/')[1] || 'Other';

                                endpoints.push({
                                    id: `${path}-${method}`,
                                    method: httpMethod,
                                    path,
                                    resource: resource.charAt(0).toUpperCase() + resource.slice(1),
                                    summary: (operation.summary as string) || undefined,
                                });
                            }
                        }
                    );
                });
            }

            return endpoints;
        } catch (error) {
            console.error('Failed to parse specification for endpoint discovery:', error);

            toast({
                description: 'The generated specification could not be parsed. Please try again.',
                title: 'Failed to parse specification',
                variant: 'destructive',
            });

            return [];
        }
    }, []);

    useEffect(() => {
        if (!jobStatus) {
            return;
        }

        if (jobStatus.status === 'COMPLETED' && jobStatus.specification) {
            jobCompletedRef.current = true;
            setSpecification(jobStatus.specification);

            const endpoints = parseEndpointsFromSpecification(jobStatus.specification);

            setDiscoveredEndpoints(endpoints);
            selectAllEndpoints();
            setIsProcessing(false);
            setJobId(null);
            nextStep();
        } else if (jobStatus.status === 'FAILED') {
            jobCompletedRef.current = true;
            setError(jobStatus.errorMessage || 'Generation failed');
            setIsProcessing(false);
            setJobId(null);
        } else if (jobStatus.status === 'CANCELLED') {
            jobCompletedRef.current = true;
            setIsProcessing(false);
            setJobId(null);
        }
    }, [
        jobStatus,
        nextStep,
        parseEndpointsFromSpecification,
        selectAllEndpoints,
        setDiscoveredEndpoints,
        setError,
        setIsProcessing,
        setJobId,
        setSpecification,
    ]);

    const isValidUrl = useCallback((urlString: string): boolean => {
        try {
            const url = new URL(urlString);

            return url.protocol === 'http:' || url.protocol === 'https:';
        } catch {
            return false;
        }
    }, []);

    const filterSpecificationBySelectedEndpoints = useCallback((): string | null => {
        if (!specification) {
            return null;
        }

        // Return full specification if all endpoints are selected
        if (selectedEndpointIds.length === discoveredEndpoints.length) {
            return specification;
        }

        try {
            const parsed = yamlParse(specification);
            const filteredPaths: Record<string, Record<string, unknown>> = {};

            if (parsed.paths) {
                Object.entries(parsed.paths).forEach(([path, methods]) => {
                    Object.entries(methods as Record<string, unknown>).forEach(([method, operation]) => {
                        const endpointId = `${path}-${method}`;

                        if (selectedEndpointIds.includes(endpointId)) {
                            if (!filteredPaths[path]) {
                                filteredPaths[path] = {};
                            }

                            filteredPaths[path][method] = operation;
                        }
                    });
                });
            }

            const filteredSpec = {
                ...parsed,
                paths: filteredPaths,
            };

            return yamlStringify(filteredSpec);
        } catch (error) {
            console.error('Failed to filter specification:', error);

            return null;
        }
    }, [discoveredEndpoints.length, selectedEndpointIds, specification]);

    const handleNext = () => {
        if (currentStep === 0) {
            if (!name || !documentationUrl) {
                return;
            }

            if (!isValidUrl(documentationUrl)) {
                toast({
                    description: 'Please enter a valid URL starting with http:// or https://',
                    title: 'Invalid URL',
                    variant: 'destructive',
                });

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

        if (selectedEndpointIds.length === 0) {
            toast({
                description: 'Please select at least one endpoint before saving.',
                title: 'No endpoints selected',
                variant: 'destructive',
            });

            return;
        }

        const filteredSpecification = filterSpecificationBySelectedEndpoints();

        if (!filteredSpecification) {
            toast({
                description: 'Failed to filter endpoints. Please try again.',
                title: 'Error',
                variant: 'destructive',
            });

            return;
        }

        importOpenApiSpecificationMutation.mutate({
            input: {
                icon: icon || undefined,
                name,
                specification: filteredSpecification,
            },
        });
    };

    const handleCancel = async () => {
        if (jobId && isProcessing) {
            try {
                await cancelGenerationMutation.mutateAsync({jobId});
            } catch (error) {
                console.error('Failed to cancel generation job:', error);

                toast({
                    description: 'The generation job may still be running on the server. You can try again later.',
                    title: 'Failed to cancel generation',
                    variant: 'destructive',
                });
            }
        }

        reset();
        navigate('/automation/settings/api-connectors');
    };

    const canProceed = (() => {
        if (currentStep === 0) {
            return !!name && !!documentationUrl;
        }

        if (currentStep === 1) {
            return selectedEndpointIds.length > 0;
        }

        return true;
    })();

    const isPending = importOpenApiSpecificationMutation.isPending || startGenerationMutation.isPending;

    return {
        canProceed,
        currentStep,
        handleCancel,
        handleNext,
        handleSave,
        isPending,
        isProcessing,
        previousStep,
    };
};

export default useApiConnectorAiPage;
