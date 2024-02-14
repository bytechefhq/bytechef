import LoadingIcon from '@/components/LoadingIcon';

/// <reference types="vite-plugin-svgr/client" />

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
import {ClipboardIcon} from 'lucide-react';
import {useState} from 'react';
import {NodeProps} from 'reactflow';
import {TYPE_ICONS} from 'shared/typeIcons';

import SchemaProperties from '../SchemaProperties';
import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

const OutputTab = ({
    currentNode,
    outputDefined = false,
    outputSchema,
    sampleOutput,
    workflowId,
}: {
    currentNode: NodeProps['data'];
    outputDefined: boolean;
    outputSchema: PropertyModel;
    workflowId: string;
    sampleOutput: object;
}) => {
    const [showUploadDialog, setShowUploadDialog] = useState(false);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

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

                    <div className="mt-2 flex items-center">
                        <div className="group flex items-center rounded-md p-1 hover:bg-gray-100">
                            <span title={outputSchema.type}>
                                {TYPE_ICONS[outputSchema.type as keyof typeof TYPE_ICONS]}
                            </span>

                            <span className="ml-2 text-sm text-gray-800">{currentNode.name}</span>

                            {sampleOutput && typeof sampleOutput !== 'object' && (
                                <div className="flex-1 text-xs text-muted-foreground">{String(sampleOutput)}</div>
                            )}

                            <ClipboardIcon
                                aria-hidden="true"
                                className="invisible mx-2 size-4 cursor-pointer text-gray-400 hover:text-gray-800 group-hover:visible"
                                onClick={() => copyToClipboard(`$\{${currentNode.name}}`)}
                            />
                        </div>
                    </div>

                    {(outputSchema as PropertyType)?.properties && (
                        <SchemaProperties
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
                                        Testing...
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
                                        Uploading...
                                    </>
                                )}

                                {!uploadSampleOutputRequestMutation.isPending && <>Upload Sample Output Data</>}
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
