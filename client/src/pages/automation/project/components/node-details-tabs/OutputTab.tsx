/// <reference types="vite-plugin-svgr/client" />

import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import useCopyToClipboard from '@/hooks/useCopyToClipboard';
import {PropertyModel} from '@/middleware/platform/configuration';
import {
    useDeleteWorkflowNodeTestOutputMutation,
    useSaveWorkflowNodeTestOutputMutation,
    useUploadSampleOutputRequestMutation,
} from '@/mutations/platform/workflowNodeTestOutputs.mutations';
import {WorkflowNodeOutputKeys} from '@/queries/platform/workflowNodeOutputs.queries';
import {PropertyType} from '@/types/types';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {NodeProps} from 'reactflow';

import PropertyField from '../PropertyField';
import SchemaProperties from '../SchemaProperties';
import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

interface OutputTabProps {
    currentNode: NodeProps['data'];
    outputDefined: boolean;
    outputSchema: PropertyModel;
    sampleOutput: object;
    workflowId: string;
}

const OutputTab = ({currentNode, outputDefined = false, outputSchema, sampleOutput, workflowId}: OutputTabProps) => {
    const [showUploadDialog, setShowUploadDialog] = useState(false);

    const [copiedValue, copyToClipboard] = useCopyToClipboard();

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
            workflowId,
            workflowNodeName: currentNode.name,
        });
    };

    const handleTestComponentClick = () => {
        saveWorkflowNodeTestOutputMutation.mutate({
            workflowId,
            workflowNodeName: currentNode.name,
        });
    };

    const handleSampleDataDialogUpload = (value: string) => {
        uploadSampleOutputRequestMutation.mutate({
            body: JSON.parse(value),
            workflowId,
            workflowNodeName: currentNode.name,
        });

        setShowUploadDialog(false);
    };

    return (
        <div className="h-full p-4">
            {outputSchema ? (
                <>
                    <div className="flex items-center justify-between">
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
                                        !uploadSampleOutputRequestMutation.isPending && <>Regenerate</>}
                                </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="w-60 cursor-pointer">
                                {outputDefined && (
                                    <DropdownMenuItem onClick={handlePredefinedOutputSchemaClick}>
                                        Use Predefined Output Schema
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem onClick={handleTestComponentClick}>Test Component</DropdownMenuItem>

                                <DropdownMenuItem onClick={() => setShowUploadDialog(true)}>
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

                    {(outputSchema as PropertyType)?.properties && (
                        <SchemaProperties
                            copiedValue={copiedValue}
                            copyToClipboard={copyToClipboard}
                            properties={(outputSchema as PropertyType).properties!}
                            sampleOutput={sampleOutput}
                            workflowNodeName={currentNode.name}
                        />
                    )}
                </>
            ) : (
                <div className="flex size-full items-center justify-center">
                    <div className="flex flex-col items-center gap-4">
                        <div>Generate Schema</div>

                        <div className="inline-flex flex-col gap-3 rounded-md shadow-sm">
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

                            <div className="text-center">or</div>

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
            />
        </div>
    );
};

export default OutputTab;
