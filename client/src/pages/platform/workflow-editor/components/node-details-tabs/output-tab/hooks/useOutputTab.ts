import {useToast} from '@/hooks/use-toast';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useSaveClusterElementTestOutputMutation} from '@/shared/middleware/graphql';
import {TriggerType} from '@/shared/middleware/platform/configuration';
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
    const [testPropertiesPopoverOpen, setTestPropertiesPopoverOpen] = useState(false);
    const [showUploadDialog, setShowUploadDialog] = useState(false);
    const [startWebhookTest, setStartWebhookTest] = useState(false);
    const [startWebhookTestDate, setStartWebhookTestDate] = useState(new Date());
    const [webhookTestCancelEnabled, setWebhookTestCancelEnabled] = useState(false);
    const [webhookTestUrl, setWebhookTestUrl] = useState<string | undefined>(undefined);

    const isClusterElement = !!clusterElementType && !!parentWorkflowNodeName;

    const {toast} = useToast();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const startWebhookTestRef = useRef(false);

    const [copiedValue, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const {webhookTriggerTestApi} = useWorkflowEditor();

    const {
        data: clusterElementOutput,
        isFetching: clusterElementOutputIsFetching,
        refetch: clusterElementOutputRefetch,
    } = useGetClusterElementOutputQuery(
        {
            clusterElementType: clusterElementType ?? '',
            clusterElementWorkflowNodeName: currentNode?.name as string,
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

    const {outputSchema, placeholder, sampleOutput} =
        workflowNodeOutput?.outputResponse || workflowNodeOutput?.variableOutputResponse || {};

    const {refetch: workflowNodeTestOutputExistsRefetch} = useCheckWorkflowNodeTestOutputExistsQuery({
        createdDate: startWebhookTestDate,
        environmentId: currentEnvironmentId,
        id: workflowId!,
        workflowNodeName: currentNode?.name as string,
    });

    const invalidateNodeOutputs = useCallback(() => {
        queryClient.invalidateQueries({
            queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
        });
    }, [queryClient, workflowId]);

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: invalidateNodeOutputs,
    });

    const saveClusterElementTestOutputMutation = useSaveClusterElementTestOutputMutation({
        onError: () => {
            toast({description: 'Failed to test cluster element. Please try again.', variant: 'destructive'});
        },
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
        deleteWorkflowNodeTestOutputMutation.mutate({
            environmentId: currentEnvironmentId,
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    }, [currentEnvironmentId, currentNode.name, deleteWorkflowNodeTestOutputMutation, workflowId]);

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
        (inputParameters: Record<string, any>) => {
            if (!clusterElementType || !parentWorkflowNodeName) {
                console.warn('handleClusterElementTestSubmit called without required cluster element context:', {
                    clusterElementType,
                    parentWorkflowNodeName,
                });

                return;
            }

            saveClusterElementTestOutputMutation.mutate(
                {
                    clusterElementType,
                    clusterElementWorkflowNodeName: currentNode.name,
                    environmentId: currentEnvironmentId,
                    inputParameters,
                    workflowId,
                    workflowNodeName: parentWorkflowNodeName,
                },
                {
                    onSuccess: () => setTestPropertiesPopoverOpen(false),
                }
            );
        },
        [
            clusterElementType,
            currentEnvironmentId,
            currentNode.name,
            parentWorkflowNodeName,
            saveClusterElementTestOutputMutation,
            workflowId,
        ]
    );

    const handleTestOperationClick = useCallback(() => {
        if (!currentNode.trigger || currentNode.triggerType === TriggerType.Polling) {
            saveWorkflowNodeTestOutputMutation.mutate({
                environmentId: currentEnvironmentId,
                id: workflowId,
                workflowNodeName: currentNode.name,
            });
        } else {
            setStartWebhookTestDate(new Date());
            setStartWebhookTest(true);

            webhookTriggerTestApi!
                .startWebhookTriggerTest({
                    environmentId: currentEnvironmentId,
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
        currentEnvironmentId,
        currentNode.name,
        currentNode.trigger,
        currentNode?.triggerType,
        saveWorkflowNodeTestOutputMutation,
        queryClient,
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
            workflowId,
        });
    }, [currentEnvironmentId, webhookTriggerTestApi, workflowId, workflowNodeOutputRefetch]);

    const hasClusterElementProperties = isClusterElement && !!currentOperationProperties?.length;

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

    return {
        copiedValue,
        copyToClipboard,
        handleClusterElementTestSubmit,
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
        setTestPropertiesPopoverOpen,
        showUploadDialog,
        testPropertiesPopoverOpen,
        testing,
        uploadSampleOutputRequestMutationPending: uploadSampleOutputRequestMutation.isPending,
        webhookTestCancelEnabled,
        webhookTestUrl,
        workflowNodeOutputIsFetching,
    };
}
