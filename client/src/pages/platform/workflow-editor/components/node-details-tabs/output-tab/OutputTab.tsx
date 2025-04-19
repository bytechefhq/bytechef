/// <reference types="vite-plugin-svgr/client" />

import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Skeleton} from '@/components/ui/skeleton';
import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {
    GetComponentDefinitionsModeTypeEnum,
    TriggerType,
    WebhookTriggerTestApi,
} from '@/shared/middleware/platform/configuration';
import {
    useDeleteWorkflowNodeTestOutputMutation,
    useSaveWorkflowNodeTestOutputMutation,
    useUploadSampleOutputRequestMutation,
} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {
    WorkflowNodeOutputKeys,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useCheckWorkflowNodeTestOutputExistsQuery} from '@/shared/queries/platform/workflowNodeTestOutputs.queries';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {AlertCircleIcon, ClipboardIcon, PenIcon} from 'lucide-react';
import {useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import PropertyField from '../../PropertyField';
import SchemaProperties from '../../SchemaProperties';
import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

interface OutputTabProps {
    connectionMissing: boolean;
    currentNode: NodeDataType;
    outputDefined: boolean;
    workflowId: string;
}

const webhookTriggerTestApi = new WebhookTriggerTestApi();

const LoadingSkeleton = () => (
    <div className="flex size-full flex-col gap-4 p-4">
        <div className="flex w-full justify-between">
            <Skeleton className="h-6 w-32" />

            <Skeleton className="h-8 w-32" />
        </div>

        <div className="flex flex-col gap-2">
            <Skeleton className="h-6 w-32" />

            <Skeleton className="ml-4 h-6 w-48" />

            <Skeleton className="ml-8 h-6 w-32" />

            <Skeleton className="ml-12 h-6 w-48" />

            <Skeleton className="h-6 w-32" />

            <Skeleton className="ml-4 h-6 w-32" />

            <Skeleton className="h-6 w-48" />

            <Skeleton className="ml-4 h-6 w-32" />

            <Skeleton className="ml-8 h-6 w-48" />
        </div>
    </div>
);

const OutputTab = ({connectionMissing, currentNode, outputDefined = false, workflowId}: OutputTabProps) => {
    const [webhookTestCancelEnabled, setWebhookTestCancelEnabled] = useState(false);
    const [showUploadDialog, setShowUploadDialog] = useState(false);
    const [startWebhookTest, setStartWebhookTest] = useState(false);
    const [startWebhookTestDate, setStartWebhookTestDate] = useState(new Date());
    const [webhookTestUrl, setWebhookTestUrl] = useState<string | undefined>(undefined);

    const {currentType} = useModeTypeStore();

    const [copiedValue, copyToClipboard] = useCopyToClipboard();
    const startWebhookTestRef = useRef(false);

    const {
        data: workflowNodeOutput,
        isFetching: workflowNodeOutputIsFetching,
        refetch: workflowNodeOutputRefetch,
    } = useGetWorkflowNodeOutputQuery({
        id: workflowId!,
        workflowNodeName: currentNode?.name as string,
    });

    const {refetch: workflowNodeTestOutputExistsRefetch} = useCheckWorkflowNodeTestOutputExistsQuery({
        createdDate: startWebhookTestDate,
        id: workflowId!,
        workflowNodeName: currentNode?.name as string,
    });

    const {outputSchema, placeholder, sampleOutput} = workflowNodeOutput || {};

    const queryClient = useQueryClient();

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
            });
        },
    });

    const saveWorkflowNodeTestOutputMutation = useSaveWorkflowNodeTestOutputMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
            });
        },
    });

    const uploadSampleOutputRequestMutation = useUploadSampleOutputRequestMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
            });

            setShowUploadDialog(false);
        },
    });

    const handlePredefinedOutputSchemaClick = useCallback(() => {
        deleteWorkflowNodeTestOutputMutation.mutate({
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    }, [currentNode.name, deleteWorkflowNodeTestOutputMutation, workflowId]);

    const handleSampleDataDialogUpload = useCallback(
        (value: string) => {
            uploadSampleOutputRequestMutation.mutate({
                body: JSON.parse(value),
                id: workflowId,
                workflowNodeName: currentNode.name,
            });
        },
        [currentNode.name, uploadSampleOutputRequestMutation, workflowId]
    );

    const handleTestOperationClick = useCallback(() => {
        if (!currentNode.trigger || (currentNode.trigger && currentNode?.triggerType === TriggerType.Polling)) {
            saveWorkflowNodeTestOutputMutation.mutate({
                id: workflowId,
                workflowNodeName: currentNode.name,
            });
        } else {
            setStartWebhookTestDate(new Date());
            setStartWebhookTest(true);

            webhookTriggerTestApi
                .startWebhookTriggerTest({
                    modeType:
                        currentType === ModeType.AUTOMATION
                            ? GetComponentDefinitionsModeTypeEnum.AUTOMATION
                            : GetComponentDefinitionsModeTypeEnum.EMBEDDED,
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
        currentNode.name,
        currentType,
        currentNode.trigger,
        currentNode?.triggerType,
        saveWorkflowNodeTestOutputMutation,
        queryClient,
        workflowId,
        workflowNodeOutputRefetch,
        workflowNodeTestOutputExistsRefetch,
    ]);

    const handleTestCancelClick = useCallback(() => {
        workflowNodeOutputRefetch();

        setStartWebhookTest(false);
        setWebhookTestCancelEnabled(false);

        webhookTriggerTestApi.stopWebhookTriggerTest({
            modeType:
                currentType === ModeType.AUTOMATION
                    ? GetComponentDefinitionsModeTypeEnum.AUTOMATION
                    : GetComponentDefinitionsModeTypeEnum.EMBEDDED,
            workflowId,
        });
    }, [currentType, workflowId, workflowNodeOutputRefetch]);

    const testing = saveWorkflowNodeTestOutputMutation.isPending || startWebhookTest;

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
        return <LoadingSkeleton />;
    }

    return (
        <div className="h-full p-4">
            {!testing && (
                <div className="h-full">
                    {outputSchema && (
                        <div className="h-full">
                            <div className="mb-2 flex items-center justify-between">
                                <h3 className="text-sm text-gray-500">Output Schema</h3>

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            disabled={saveWorkflowNodeTestOutputMutation.isPending}
                                            size="sm"
                                            variant="outline"
                                        >
                                            <PenIcon /> Define <CaretDownIcon className="ml-0.5" />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end" className="w-60 cursor-pointer">
                                        {outputDefined && (
                                            <DropdownMenuItem
                                                className="cursor-pointer"
                                                onClick={handlePredefinedOutputSchemaClick}
                                            >
                                                Use Predefined Output Schema
                                            </DropdownMenuItem>
                                        )}

                                        <DropdownMenuItem
                                            className="cursor-pointer"
                                            disabled={connectionMissing}
                                            onClick={handleTestOperationClick}
                                        >
                                            {`Test ${currentNode.trigger ? 'Trigger' : 'Action'}`}
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="cursor-pointer"
                                            onClick={() => setShowUploadDialog(true)}
                                        >
                                            Upload Sample Output Data
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>

                            <PropertyField
                                copiedValue={copiedValue}
                                copyToClipboard={copyToClipboard}
                                label={currentNode.name}
                                property={outputSchema}
                                sampleOutput={sampleOutput}
                                valueToCopy={`$\{${currentNode.name}}`}
                                workflowNodeName={currentNode.name}
                            />

                            {(outputSchema as PropertyAllType)?.properties && sampleOutput && (
                                <SchemaProperties
                                    copiedValue={copiedValue}
                                    copyToClipboard={copyToClipboard}
                                    properties={(outputSchema as PropertyAllType).properties!}
                                    sampleOutput={sampleOutput}
                                    workflowNodeName={currentNode.name}
                                />
                            )}

                            {(outputSchema as PropertyAllType)?.items && sampleOutput && (
                                <div className="ml-3 flex flex-col overflow-y-auto border-l border-l-border/50 pl-1">
                                    <SchemaProperties
                                        copiedValue={copiedValue}
                                        copyToClipboard={copyToClipboard}
                                        properties={(outputSchema as PropertyAllType).items!}
                                        sampleOutput={sampleOutput}
                                        workflowNodeName={currentNode.name}
                                    />
                                </div>
                            )}
                        </div>
                    )}

                    {!outputSchema && (
                        <div className="flex size-full items-center justify-center">
                            <div className="flex flex-col items-center gap-8">
                                <div className="flex w-full flex-col gap-1">
                                    <div className="self-center">Define Output Schema</div>

                                    <p className="text-sm text-muted-foreground">
                                        Define the expected output schema with one of the methods
                                    </p>
                                </div>

                                <div className="flex flex-col gap-4">
                                    <div className="flex w-full flex-col gap-3">
                                        <Button
                                            disabled={saveWorkflowNodeTestOutputMutation.isPending}
                                            onClick={handleTestOperationClick}
                                            type="button"
                                        >
                                            {`Test ${currentNode.trigger ? 'Trigger' : 'Action'}`}
                                        </Button>

                                        <span className="text-center">or</span>
                                    </div>

                                    <Button
                                        disabled={uploadSampleOutputRequestMutation.isPending}
                                        onClick={() => setShowUploadDialog(true)}
                                        type="button"
                                    >
                                        {uploadSampleOutputRequestMutation.isPending && <LoadingSkeleton />}

                                        {!uploadSampleOutputRequestMutation.isPending && (
                                            <span>Upload Sample Output Data</span>
                                        )}
                                    </Button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {testing && (
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
                        <div className={twMerge('flex items-center justify-center', !currentNode.trigger && 'w-full')}>
                            <LoadingSkeleton />
                        </div>

                        {currentNode.trigger &&
                            currentNode.triggerType !== TriggerType.Polling &&
                            currentNode.triggerType !== TriggerType.Hybrid && (
                                <Button
                                    className="flex items-center gap-2"
                                    disabled={!webhookTestCancelEnabled}
                                    onClick={handleTestCancelClick}
                                    size="sm"
                                    variant="outline"
                                >
                                    Cancel
                                </Button>
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
                                            Please go to your service and make an action that will activate this trigger
                                        </div>
                                    )}
                                </AlertDescription>
                            </Alert>
                        )}
                </div>
            )}

            <OutputTabSampleDataDialog
                onClose={() => setShowUploadDialog(false)}
                onUpload={handleSampleDataDialogUpload}
                open={showUploadDialog}
                placeholder={placeholder || sampleOutput}
            />
        </div>
    );
};

export default OutputTab;
