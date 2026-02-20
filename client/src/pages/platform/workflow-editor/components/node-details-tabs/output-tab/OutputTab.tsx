/// <reference types="vite-plugin-svgr/client" />

import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Input} from '@/components/ui/input';
import ClusterElementTestPropertiesPopover from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/ClusterElementTestPropertiesPopover';
import OutputSchemaCreationControls from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaCreationControls';
import OutputSchemaDisplay from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaDisplay';
import OutputTabSampleDataDialog from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import DialogLoader from '@/shared/components/DialogLoader';
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
import {AlertCircleIcon, ClipboardIcon} from 'lucide-react';
import {Suspense, useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface OutputTabProps {
    clusterElementType?: string;
    connectionMissing: boolean;
    currentNode: NodeDataType;
    currentOperationProperties?: PropertyAllType[];
    outputDefined?: boolean;
    outputFunctionDefined?: boolean;
    parentWorkflowNodeName?: string;
    variablePropertiesDefined?: boolean;
    workflowId: string;
}

const OutputTab = ({
    clusterElementType,
    connectionMissing,
    currentNode,
    currentOperationProperties,
    outputDefined,
    outputFunctionDefined,
    parentWorkflowNodeName,
    variablePropertiesDefined = false,
    workflowId,
}: OutputTabProps) => {
    const [testPropertiesPopoverOpen, setTestPropertiesPopoverOpen] = useState(false);
    const [showUploadDialog, setShowUploadDialog] = useState(false);
    const [startWebhookTest, setStartWebhookTest] = useState(false);
    const [startWebhookTestDate, setStartWebhookTestDate] = useState(new Date());
    const [webhookTestCancelEnabled, setWebhookTestCancelEnabled] = useState(false);
    const [webhookTestUrl, setWebhookTestUrl] = useState<string | undefined>(undefined);

    const isClusterElement = !!clusterElementType && !!parentWorkflowNodeName;

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
            clusterElementType: clusterElementType!,
            clusterElementWorkflowNodeName: currentNode?.name as string,
            environmentId: currentEnvironmentId,
            id: workflowId!,
            workflowNodeName: parentWorkflowNodeName!,
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

    function renderClusterElementTestButton() {
        if (!hasClusterElementProperties || !currentOperationProperties) {
            return undefined;
        }

        return (
            <ClusterElementTestPropertiesPopover
                currentNode={currentNode}
                onOpenChange={setTestPropertiesPopoverOpen}
                onSubmit={handleClusterElementTestSubmit}
                open={testPropertiesPopoverOpen}
                properties={currentOperationProperties}
            >
                <Button
                    disabled={connectionMissing || saveClusterElementTestOutputMutation.isPending}
                    label="Test Action"
                    variant="outline"
                />
            </ClusterElementTestPropertiesPopover>
        );
    }

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

    if (!testing && workflowNodeOutputIsFetching) {
        return <></>;
    }

    if (!testing && outputFunctionDefined && !outputSchema) {
        return <div className="p-4 text-sm text-muted-foreground">No output schema to show.</div>;
    }

    return (
        <div className="h-full p-4">
            {!testing && (
                <div className="h-full">
                    {outputSchema && (
                        <OutputSchemaDisplay
                            connectionMissing={connectionMissing}
                            copiedValue={copiedValue}
                            copyToClipboard={copyToClipboard}
                            currentNode={currentNode}
                            handlePredefinedOutputSchemaClick={handlePredefinedOutputSchemaClick}
                            handleTestOperationClick={handleTestOperationClick}
                            outputSchema={outputSchema}
                            sampleOutput={sampleOutput}
                            saveWorkflowNodeTestOutputMutation={saveWorkflowNodeTestOutputMutation}
                            setShowUploadDialog={setShowUploadDialog}
                            testActionButton={renderClusterElementTestButton()}
                            variablePropertiesDefined={variablePropertiesDefined}
                        />
                    )}

                    {!outputSchema && (
                        <OutputSchemaCreationControls
                            handleTestOperationClick={handleTestOperationClick}
                            outputDefined={outputDefined}
                            saveWorkflowNodeTestOutputMutationPending={saveWorkflowNodeTestOutputMutation.isPending}
                            setShowUploadDialog={setShowUploadDialog}
                            showUploadSampleOutputButton={outputDefined}
                            testActionButton={renderClusterElementTestButton()}
                            trigger={currentNode.trigger}
                            uploadSampleOutputRequestMutationPending={uploadSampleOutputRequestMutation.isPending}
                            variablePropertiesDefined={variablePropertiesDefined}
                        />
                    )}
                </div>
            )}

            {testing && (
                <Suspense
                    fallback={
                        <div className="flex items-center justify-center p-4">
                            <LoadingIcon />

                            <span>Loading Testing UI...</span>
                        </div>
                    }
                >
                    <div className="flex size-full flex-col items-center justify-center gap-6">
                        <div
                            className={twMerge(
                                'flex',
                                currentNode.triggerType !== TriggerType.Polling &&
                                    currentNode.triggerType !== TriggerType.Hybrid &&
                                    'w-full justify-between pl-2',
                                (currentNode.triggerType === TriggerType.Polling ||
                                    currentNode.triggerType === TriggerType.Hybrid) &&
                                    'flex-col gap-2'
                            )}
                        >
                            <div
                                className={twMerge(
                                    'mt-1 flex items-center justify-center',
                                    !currentNode.trigger && 'w-full'
                                )}
                            >
                                <LoadingIcon />

                                <span className="text-lg">{`Testing ${currentNode.trigger ? 'Trigger' : 'Action'}`}</span>
                            </div>

                            {currentNode.trigger &&
                                currentNode.triggerType !== TriggerType.Polling &&
                                currentNode.triggerType !== TriggerType.Hybrid && (
                                    <Button
                                        className="flex items-center gap-2"
                                        disabled={!webhookTestCancelEnabled}
                                        label="Cancel"
                                        onClick={handleTestCancelClick}
                                        size="sm"
                                        variant="outline"
                                    />
                                )}
                        </div>

                        {currentNode.trigger &&
                            currentNode.triggerType !== TriggerType.Polling &&
                            currentNode.triggerType !== TriggerType.Hybrid && (
                                <Alert>
                                    <AlertCircleIcon className="size-4" />

                                    <AlertTitle>Action Required</AlertTitle>

                                    <AlertDescription className="flex flex-col gap-1">
                                        {currentNode.triggerType === TriggerType.StaticWebhook ? (
                                            <>
                                                <div>Please call the following webhook test URL</div>
                                                <div className="relative">
                                                    <Input className="pr-8" disabled value={webhookTestUrl} />

                                                    <ClipboardIcon
                                                        aria-hidden="true"
                                                        className="absolute right-0 top-2.5 mx-2 size-4 cursor-pointer text-gray-400 hover:text-gray-800 group-hover:visible"
                                                        onClick={() => copyToClipboard(webhookTestUrl!)}
                                                    />
                                                </div>
                                                <div>by sending sample data</div>{' '}
                                            </>
                                        ) : (
                                            <div>
                                                Please go to your service and make an action that will activate this
                                                trigger
                                            </div>
                                        )}
                                    </AlertDescription>
                                </Alert>
                            )}
                    </div>
                </Suspense>
            )}

            {showUploadDialog && (
                <Suspense fallback={<DialogLoader />}>
                    <OutputTabSampleDataDialog
                        onClose={() => setShowUploadDialog(false)}
                        onUpload={handleSampleDataDialogUpload}
                        open={showUploadDialog}
                        placeholder={placeholder || sampleOutput}
                    />
                </Suspense>
            )}
        </div>
    );
};

export default OutputTab;
