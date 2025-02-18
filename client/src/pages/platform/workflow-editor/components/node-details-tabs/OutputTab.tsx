/// <reference types="vite-plugin-svgr/client" />

import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {
    useDeleteWorkflowNodeTestOutputMutation,
    useSaveWorkflowNodeTestOutputMutation,
    useUploadSampleOutputRequestMutation,
} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {
    WorkflowNodeOutputKeys,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {useCallback, useState} from 'react';

import PropertyField from '../PropertyField';
import SchemaProperties from '../SchemaProperties';
import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

interface OutputTabProps {
    connectionMissing: boolean;
    currentNode: NodeDataType;
    outputDefined: boolean;
    workflowId: string;
}

const OutputTab = ({connectionMissing, currentNode, outputDefined = false, workflowId}: OutputTabProps) => {
    const [showUploadDialog, setShowUploadDialog] = useState(false);

    const [copiedValue, copyToClipboard] = useCopyToClipboard();

    const {data: workflowNodeOutput, isFetching: workflowNodeOutputIsFetching} = useGetWorkflowNodeOutputQuery({
        id: workflowId!,
        workflowNodeName: currentNode?.name as string,
    });

    const {outputSchema, sampleOutput} = workflowNodeOutput || {};

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
        },
    });

    const handlePredefinedOutputSchemaClick = useCallback(() => {
        deleteWorkflowNodeTestOutputMutation.mutate({
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    }, [currentNode.name, deleteWorkflowNodeTestOutputMutation, workflowId]);

    const handleTestComponentClick = useCallback(() => {
        saveWorkflowNodeTestOutputMutation.mutate({
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    }, [currentNode.name, saveWorkflowNodeTestOutputMutation, workflowId]);

    const handleSampleDataDialogUpload = useCallback(
        (value: string) => {
            uploadSampleOutputRequestMutation.mutate({
                body: JSON.parse(value),
                id: workflowId,
                workflowNodeName: currentNode.name,
            });

            setShowUploadDialog(false);
        },
        [currentNode.name, uploadSampleOutputRequestMutation, workflowId]
    );

    if (workflowNodeOutputIsFetching) {
        return (
            <div className="flex size-full items-center justify-center">
                <LoadingIcon /> Loading...
            </div>
        );
    }

    return (
        <div className="h-full p-4">
            {outputSchema && (
                <>
                    <div className="mb-2 flex items-center justify-between">
                        <h3 className="text-sm text-gray-500">Output Schema</h3>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    disabled={saveWorkflowNodeTestOutputMutation.isPending}
                                    size="sm"
                                    variant="outline"
                                >
                                    {(saveWorkflowNodeTestOutputMutation.isPending ||
                                        uploadSampleOutputRequestMutation.isPending) && (
                                        <>
                                            <LoadingIcon />
                                            Testing...
                                        </>
                                    )}

                                    {!saveWorkflowNodeTestOutputMutation.isPending &&
                                        !uploadSampleOutputRequestMutation.isPending && (
                                            <>
                                                Define <CaretDownIcon className="ml-0.5" />
                                            </>
                                        )}
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

                                {!currentNode.trigger && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        disabled={connectionMissing}
                                        onClick={handleTestComponentClick}
                                    >
                                        Test Action
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem className="cursor-pointer" onClick={() => setShowUploadDialog(true)}>
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
                </>
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
                            {!currentNode.trigger && (
                                <div className="flex w-full flex-col gap-3">
                                    <Button
                                        disabled={saveWorkflowNodeTestOutputMutation.isPending}
                                        onClick={handleTestComponentClick}
                                        type="button"
                                    >
                                        {saveWorkflowNodeTestOutputMutation.isPending && (
                                            <>
                                                <LoadingIcon />

                                                <span>Testing...</span>
                                            </>
                                        )}

                                        {!saveWorkflowNodeTestOutputMutation.isPending &&
                                            `Test ${currentNode.trigger ? 'Trigger' : 'Action'}`}
                                    </Button>

                                    <span className="text-center">or</span>
                                </div>
                            )}

                            <Button
                                disabled={uploadSampleOutputRequestMutation.isPending}
                                onClick={() => setShowUploadDialog(true)}
                                type="button"
                            >
                                {uploadSampleOutputRequestMutation.isPending && (
                                    <>
                                        <LoadingIcon />

                                        <span>Uploading...</span>
                                    </>
                                )}

                                {!uploadSampleOutputRequestMutation.isPending && <span>Upload Sample Output Data</span>}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            <OutputTabSampleDataDialog
                onClose={() => setShowUploadDialog(false)}
                onUpload={handleSampleDataDialogUpload}
                open={showUploadDialog}
                sampleOutput={sampleOutput}
            />
        </div>
    );
};

export default OutputTab;
