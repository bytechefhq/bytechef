/// <reference types="vite-plugin-svgr/client" />

import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {PropertyModel} from '@/middleware/platform/configuration';
import {
    useSaveWorkflowNodeTestOutputMutation,
    useUploadSampleOutputRequestMutation,
} from '@/mutations/platform/workflowNodeTestOutputs.mutations';
import {WorkflowNodeOutputKeys} from '@/queries/platform/workflowNodeOutputs.queries';
import {PropertyType} from '@/types/projectTypes';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {NodeProps} from 'reactflow';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';

import OutputTabSampleDataDialog from './OutputTabSampleDataDialog';

const AnimateSpin = ({className}: {className?: string}) => (
    <svg
        className={twMerge('animate-spin -ml-1 mr-3 h-5 w-5', className)}
        fill="none"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
    >
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>

        <path
            className="opacity-75"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            fill="currentColor"
        ></path>
    </svg>
);

const PropertyField = ({data, label = 'item'}: {data: PropertyType; label: string}) => (
    <div className="inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
        <span title={data.type}>{TYPE_ICONS[data.type as keyof typeof TYPE_ICONS]}</span>

        <span className="pl-2">{label}</span>
    </div>
);

const SchemaProperties = ({properties}: {properties: Array<PropertyType>}) => (
    <ul className="ml-2 h-full">
        {properties.map((property, index) => (
            <li className="flex flex-col" key={`${property.name}_${index}`}>
                <PropertyField data={property} label={property.name!} />

                {property.properties && !!property.properties.length && (
                    <div
                        className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                        key={property.name}
                    >
                        <SchemaProperties properties={property.properties} />
                    </div>
                )}

                {property.items && !!property.items.length && (
                    <div
                        className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                        key={property.name}
                    >
                        <SchemaProperties properties={property.items} />
                    </div>
                )}
            </li>
        ))}
    </ul>
);

const OutputTab = ({
    currentNode,
    outputSchema,
    workflowId,
}: {
    currentNode: NodeProps['data'];
    outputSchema: PropertyModel;
    workflowId: string;
}) => {
    const [showUploadDialog, setShowUploadDialog] = useState(false);

    const queryClient = useQueryClient();

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

    const handleTestComponentClick = () => {
        saveWorkflowNodeTestOutputMutation.mutate({
            workflowId,
            workflowNodeName: currentNode.name,
        });
    };

    const handleSampleDataDialogUpload = (value: string) => {
        console.log(JSON.parse(value));
        uploadSampleOutputRequestMutation.mutate({
            uploadWorkflowNodeSampleOutputRequestModel: {
                sampleOutput: JSON.parse(value),
            },
            workflowId,
            workflowNodeName: currentNode.name,
        });

        setShowUploadDialog(false);
    };

    return (
        <div className="h-full p-4">
            {outputSchema ? (
                <>
                    <div className="mt-2 flex items-center">
                        <div className="flex items-center">
                            <span title={outputSchema.type}>
                                {TYPE_ICONS[outputSchema.type as keyof typeof TYPE_ICONS]}
                            </span>

                            <span className="ml-2 text-sm text-gray-800">{currentNode.name}</span>
                        </div>

                        <div className="absolute right-4">
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button disabled={saveWorkflowNodeTestOutputMutation.isPending} variant="outline">
                                        {(saveWorkflowNodeTestOutputMutation.isPending ||
                                            uploadSampleOutputRequestMutation.isPending) && (
                                            <>
                                                <AnimateSpin />
                                                Testing...
                                            </>
                                        )}

                                        {!saveWorkflowNodeTestOutputMutation.isPending &&
                                            !uploadSampleOutputRequestMutation.isPending && <>Regenerate</>}
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end" className="w-56 cursor-pointer">
                                    <DropdownMenuItem onClick={handleTestComponentClick}>
                                        Test Component
                                    </DropdownMenuItem>

                                    <DropdownMenuItem onClick={() => setShowUploadDialog(true)}>
                                        Upload Sample Value
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>
                    </div>

                    {(outputSchema as PropertyType)?.properties && (
                        <SchemaProperties properties={(outputSchema as PropertyType).properties!} />
                    )}

                    {!(outputSchema as PropertyType).properties && !!(outputSchema as PropertyType).controlType && (
                        <PropertyField data={outputSchema} label={(outputSchema as PropertyType).controlType!} />
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
                                        <AnimateSpin />
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
                                        <AnimateSpin />
                                        Uploading...
                                    </>
                                )}

                                {!uploadSampleOutputRequestMutation.isPending && <>Upload Sample Value</>}
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
