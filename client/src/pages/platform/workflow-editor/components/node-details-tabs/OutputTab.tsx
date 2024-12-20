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
import {useState} from 'react';

import PropertyField from '../PropertyField';
import SchemaProperties from '../SchemaProperties';
import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

interface OutputTabProps {
    connectionMissing: boolean;
    currentNode: NodeDataType;
    outputDefined: boolean;
    outputFunctionDefined: boolean;
    workflowId: string;
}

const OutputTab = ({
    connectionMissing,
    currentNode,
    outputDefined = false,
    outputFunctionDefined = false,
    workflowId,
}: OutputTabProps) => {
    const [showUploadDialog, setShowUploadDialog] = useState(false);

    const [copiedValue, copyToClipboard] = useCopyToClipboard();

    const {data: workflowNodeOutput} = useGetWorkflowNodeOutputQuery({
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

    const handlePredefinedOutputSchemaClick = () => {
        deleteWorkflowNodeTestOutputMutation.mutate({
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    };

    const handleTestComponentClick = () => {
        saveWorkflowNodeTestOutputMutation.mutate({
            id: workflowId,
            workflowNodeName: currentNode.name,
        });
    };

    const handleSampleDataDialogUpload = (value: string) => {
        uploadSampleOutputRequestMutation.mutate({
            body: JSON.parse(value),
            id: workflowId,
            workflowNodeName: currentNode.name,
        });

        setShowUploadDialog(false);
    };

    return (
        <div className="h-full p-4">
            {outputSchema ? (
                <>
                    <div className="mb-2 flex items-center justify-between">
                        <div className="text-sm font-semibold">Output Schema</div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button disabled={saveWorkflowNodeTestOutputMutation.isPending} variant="outline">
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
                                                Regenerate <CaretDownIcon className="ml-0.5" />
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

                                {!currentNode.trigger && !outputFunctionDefined && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        disabled={connectionMissing}
                                        onClick={handleTestComponentClick}
                                    >
                                        Test Component
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
            ) : (
                <div className="flex size-full items-center justify-center">
                    <div className="flex flex-col items-center gap-4">
                        <span className="self-center">Generate Schema</span>

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

                                    {!saveWorkflowNodeTestOutputMutation.isPending && <>Test component</>}
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
