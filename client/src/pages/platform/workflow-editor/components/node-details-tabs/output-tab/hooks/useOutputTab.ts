import {convertNameToSnakeCase} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import invalidateWorkflowValidation from '@/pages/platform/workflow-editor/utils/invalidateWorkflowValidation';
import {useSaveClusterElementTestOutputMutation} from '@/shared/middleware/graphql';
import {ResponseError, TriggerType} from '@/shared/middleware/platform/configuration';
import {
    useDeleteWorkflowNodeTestOutputMutation,
    useSaveWorkflowNodeTestOutputMutation,
    useUploadSampleOutputRequestMutation,
} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {
    WorkflowNodeOutputKeys,
    useGetClusterElementOutputQuery,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useCheckWorkflowNodeTestOutputExistsQuery} from '@/shared/queries/platform/workflowNodeTestOutputs.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {useCallback, useEffect, useRef, useState} from 'react';

export interface TestOutputErrorI {
    message: string;
    title: string;
}

async function resolveErrorMessage(error: unknown): Promise<string> {
    if (error instanceof ResponseError) {
        const problem: {detail?: string; title?: string} | null = await error.response
            .clone()
            .json()
            .catch(() => null);

        return problem?.detail || problem?.title || `Request failed with status ${error.response.status}`;
    }

    return (error instanceof Error && error.message) || 'Unknown error';
}

interface UseOutputTabProps {
    clusterElementType?: string;
    currentNode: NodeDataType;
    currentOperationProperties?: PropertyAllType[];
    parentWorkflowNodeName?: string;
    workflowId: string;
}

export default function useOutputTab({
    clusterElementType,
    currentNode,
    currentOperationProperties,
    parentWorkflowNodeName,
    workflowId,
}: UseOutputTabProps) {
    const [noOutputWorkflowNodeName, setNoOutputWorkflowNodeName] = useState<string | undefined>(undefined);
    const [showUploadDialog, setShowUploadDialog] = useState(false);
    const [startWebhookTest, setStartWebhookTest] = useState(false);
    const [startWebhookTestDate, setStartWebhookTestDate] = useState(new Date());
    const [testOutputError, setTestOutputError] = useState<TestOutputErrorI | undefined>(undefined);
    const [webhookTestCancelEnabled, setWebhookTestCancelEnabled] = useState(false);
    const [webhookTestUrl, setWebhookTestUrl] = useState<string | undefined>(undefined);

    const isClusterElement = !!clusterElementType && !!parentWorkflowNodeName;

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const startWebhookTestRef = useRef(false);
    const testOutputErrorGenerationRef = useRef(0);

    const [copiedValue, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const {webhookTriggerTestApi} = useWorkflowEditor();

    const {
        data: clusterElementOutput,
        isFetching: clusterElementOutputIsFetching,
        refetch: clusterElementOutputRefetch,
    } = useGetClusterElementOutputQuery(
        {
            clusterElementType: clusterElementType ? convertNameToSnakeCase(clusterElementType) : '',
            clusterElementWorkflowNodeName: currentNode?.workflowNodeName,
            environmentId: currentEnvironmentId,
            id: workflowId!,
            workflowNodeName: parentWorkflowNodeName ?? '',
        },
        isClusterElement
    );

    const {
        data: regularNodeOutput,
        isFetching: regularNodeOutputIsFetching,
        refetch: regularNodeOutputRefetch,
    } = useGetWorkflowNodeOutputQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflowId!,
            workflowNodeName: currentNode?.name as string,
        },
        !isClusterElement
    );

    const workflowNodeOutput = isClusterElement ? clusterElementOutput : regularNodeOutput;
    const workflowNodeOutputIsFetching = isClusterElement
        ? clusterElementOutputIsFetching
        : regularNodeOutputIsFetching;
    const workflowNodeOutputRefetch = isClusterElement ? clusterElementOutputRefetch : regularNodeOutputRefetch;

    const {outputSchema, placeholder, sampleOutput} = workflowNodeOutput?.outputResponse || {};
    const {outputSchema: variableOutputSchema, sampleOutput: variableSampleOutput} =
        workflowNodeOutput?.variableOutputResponse || {};
    const testOutputResponse = !!workflowNodeOutput?.testOutputResponse;

    const {refetch: workflowNodeTestOutputExistsRefetch} = useCheckWorkflowNodeTestOutputExistsQuery(
        {
            createdDate: startWebhookTestDate,
            environmentId: currentEnvironmentId,
            id: workflowId!,
            workflowNodeName: currentNode?.name as string,
        },
        !isClusterElement
    );

    const invalidateNodeOutputs = useCallback(() => {
        queryClient.invalidateQueries({
            queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
        });

        invalidateWorkflowValidation(queryClient);
    }, [queryClient, workflowId]);

    const clearTestOutputError = useCallback(() => {
        testOutputErrorGenerationRef.current += 1;

        setTestOutputError(undefined);

        return testOutputErrorGenerationRef.current;
    }, []);

    const showTestOutputError = useCallback((title: string, error: unknown, generation: number) => {
        resolveErrorMessage(error).then((message) => {
            if (generation === testOutputErrorGenerationRef.current) {
                setTestOutputError({message, title});
            }
        });
    }, []);

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const saveClusterElementTestOutputMutation = useSaveClusterElementTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const saveWorkflowNodeTestOutputMutation = useSaveWorkflowNodeTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const uploadSampleOutputRequestMutation = useUploadSampleOutputRequestMutation({
        onSuccess: () => {
            invalidateNodeOutputs();

            setShowUploadDialog(false);
        },
    });

    const handlePredefinedOutputSchemaClick = useCallback(() => {
        const generation = clearTestOutputError();

        deleteWorkflowNodeTestOutputMutation.mutate(
            {
                environmentId: currentEnvironmentId,
                id: workflowId,
                workflowNodeName: currentNode.name,
            },
            {
                onError: (error) => showTestOutputError('Reset failed', error, generation),
            }
        );
    }, [
        clearTestOutputError,
        currentEnvironmentId,
        currentNode.name,
        deleteWorkflowNodeTestOutputMutation,
        showTestOutputError,
        workflowId,
    ]);

    const handleSampleDataDialogUpload = useCallback(
        (value: string) => {
            uploadSampleOutputRequestMutation.mutate({
                body: JSON.parse(value),
                environmentId: currentEnvironmentId,
                id: workflowId,
                workflowNodeName: currentNode.name,
            });
        },
        [currentEnvironmentId, currentNode.name, uploadSampleOutputRequestMutation, workflowId]
    );

    const handleClusterElementTestSubmit = useCallback(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (inputParameters: Record<string, any>, onSuccess?: () => void) => {
            if (!clusterElementType || !parentWorkflowNodeName) {
                console.warn('handleClusterElementTestSubmit called without required cluster element context:', {
                    clusterElementType,
                    parentWorkflowNodeName,
                });

                return;
            }

            const generation = clearTestOutputError();

            saveClusterElementTestOutputMutation.mutate(
                {
                    clusterElementType,
                    clusterElementWorkflowNodeName: currentNode.workflowNodeName,
                    environmentId: currentEnvironmentId,
                    inputParameters,
                    workflowId,
                    workflowNodeName: parentWorkflowNodeName,
                },
                {
                    onError: (error) => showTestOutputError('Test failed', error, generation),
                    onSuccess,
                }
            );
        },
        [
            clearTestOutputError,
            clusterElementType,
            currentEnvironmentId,
            currentNode.workflowNodeName,
            parentWorkflowNodeName,
            saveClusterElementTestOutputMutation,
            showTestOutputError,
            workflowId,
        ]
    );

    const handleTestOperationClick = useCallback(() => {
        const generation = clearTestOutputError();

        setNoOutputWorkflowNodeName(undefined);

        if (!currentNode.trigger || currentNode.triggerType === TriggerType.Polling) {
            saveWorkflowNodeTestOutputMutation.mutate(
                {
                    environmentId: currentEnvironmentId,
                    id: workflowId,
                    workflowNodeName: currentNode.name,
                },
                {
                    onError: (error) => showTestOutputError('Test failed', error, generation),
                    onSuccess: (workflowNodeTestOutput, variables) => {
                        if (!workflowNodeTestOutput) {
                            setNoOutputWorkflowNodeName(variables.workflowNodeName);
                        }
                    },
                }
            );
        } else {
            setStartWebhookTestDate(new Date());
            setStartWebhookTest(true);

            webhookTriggerTestApi!
                .startWebhookTriggerTest({
                    environmentId: currentEnvironmentId,
                    triggerName: currentNode.name,
                    workflowId,
                })
                .then((response) => {
                    setWebhookTestUrl(response.webhookUrl);
                    setWebhookTestCancelEnabled(true);

                    function check() {
                        setTimeout(() => {
                            if (!startWebhookTestRef.current) {
                                return;
                            }

                            workflowNodeTestOutputExistsRefetch().then((result) => {
                                if (result.data?._exists) {
                                    queryClient.invalidateQueries({
                                        queryKey: [
                                            ...WorkflowNodeOutputKeys.workflowNodeOutput({
                                                environmentId: currentEnvironmentId,
                                                id: workflowId,
                                                workflowNodeName: currentNode.name,
                                            }),
                                        ],
                                    });

                                    invalidateWorkflowValidation(queryClient);

                                    workflowNodeOutputRefetch().then(() => {
                                        setStartWebhookTest(false);
                                    });
                                } else {
                                    check();
                                }
                            });
                        }, 3000);
                    }

                    check();
                })
                .catch(() => {
                    setStartWebhookTest(false);
                });
        }
    }, [
        clearTestOutputError,
        currentEnvironmentId,
        currentNode.name,
        currentNode.trigger,
        currentNode?.triggerType,
        saveWorkflowNodeTestOutputMutation,
        queryClient,
        showTestOutputError,
        webhookTriggerTestApi,
        workflowId,
        workflowNodeOutputRefetch,
        workflowNodeTestOutputExistsRefetch,
    ]);

    const handleTestCancelClick = useCallback(() => {
        workflowNodeOutputRefetch();

        setStartWebhookTest(false);
        setWebhookTestCancelEnabled(false);

        webhookTriggerTestApi!.stopWebhookTriggerTest({
            environmentId: currentEnvironmentId,
            triggerName: currentNode.name,
            workflowId,
        });
    }, [currentEnvironmentId, currentNode.name, webhookTriggerTestApi, workflowId, workflowNodeOutputRefetch]);

    const handleNoOutputNoticeDismiss = useCallback(() => setNoOutputWorkflowNodeName(undefined), []);

    const hasClusterElementProperties = isClusterElement && !!currentOperationProperties?.length;

    const testReturnedNoOutput = !!noOutputWorkflowNodeName && noOutputWorkflowNodeName === currentNode.name;

    const testing =
        saveClusterElementTestOutputMutation.isPending ||
        saveWorkflowNodeTestOutputMutation.isPending ||
        startWebhookTest;

    useEffect(() => {
        return () => {
            setStartWebhookTest(false);
            startWebhookTestRef.current = false;
        };
    }, []);

    useEffect(() => {
        startWebhookTestRef.current = startWebhookTest;
    }, [startWebhookTest]);

    useEffect(() => {
        clearTestOutputError();
    }, [clearTestOutputError, currentNode.name]);

    return {
        clearTestOutputError,
        copiedValue,
        copyToClipboard,
        handleClusterElementTestSubmit,
        handleNoOutputNoticeDismiss,
        handlePredefinedOutputSchemaClick,
        handleSampleDataDialogUpload,
        handleTestCancelClick,
        handleTestOperationClick,
        hasClusterElementProperties,
        outputSchema,
        placeholder,
        sampleOutput,
        saveClusterElementTestOutputMutationPending: saveClusterElementTestOutputMutation.isPending,
        saveWorkflowNodeTestOutputMutation,
        saveWorkflowNodeTestOutputMutationPending: saveWorkflowNodeTestOutputMutation.isPending,
        setShowUploadDialog,
        showUploadDialog,
        testOutputError,
        testOutputResponse,
        testReturnedNoOutput,
        testing,
        uploadSampleOutputRequestMutationPending: uploadSampleOutputRequestMutation.isPending,
        variableOutputSchema,
        variableSampleOutput,
        webhookTestCancelEnabled,
        webhookTestUrl,
        workflowNodeOutputIsFetching,
    };
}
