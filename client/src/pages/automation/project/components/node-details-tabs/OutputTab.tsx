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
import getNestedObject from '@/pages/automation/project/utils/getNestedObject';
import {WorkflowNodeOutputKeys} from '@/queries/platform/workflowNodeOutputs.queries';
import {PropertyType} from '@/types/projectTypes';
import {useQueryClient} from '@tanstack/react-query';
import {ClipboardIcon} from 'lucide-react';
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

const PropertyField = ({
    label = '[index]',
    parentPath,
    property,
    sampleOutput,
    workflowNodeName,
}: {
    label: string;
    property: PropertyType;
    parentPath?: string;
    sampleOutput: object;
    workflowNodeName: string;
}) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const selector = `${parentPath ? parentPath + '.' : ''}${property.name}`.replace('/', '.');

    const value = property.name && getNestedObject(sampleOutput, selector);

    return (
        <div>
            <div className="group relative inline-flex items-center rounded-md p-1 pr-5 text-sm hover:bg-gray-100">
                <span title={property.type}>{TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}</span>

                <span className="px-2">{label}</span>

                {(value || value === 0 || value === false) && typeof value !== 'object' && (
                    <div className="flex-1 text-xs text-muted-foreground">
                        {value === true ? 'true' : value === false ? false : value}
                    </div>
                )}

                <div className="absolute right-0.5">
                    <ClipboardIcon
                        aria-hidden="true"
                        className="invisible size-4 cursor-pointer bg-background text-gray-400 group-hover:visible"
                        onClick={() => copyToClipboard(`$\{${workflowNodeName}.${selector}}`)}
                    />
                </div>
            </div>
        </div>
    );
};

const SchemaProperties = ({
    parentPath,
    properties,
    sampleOutput,
    workflowNodeName,
}: {
    parentPath?: string;
    properties: Array<PropertyType>;
    sampleOutput: object;
    workflowNodeName: string;
}) => (
    <ul className="ml-2 h-full">
        {properties.map((property, index) => {
            const path = `${parentPath ? parentPath + (property.name ? '.' : '') : ''}${property.name || '[index]'}`;

            return (
                <li className="flex flex-col" key={`${property.name}_${index}`}>
                    <PropertyField
                        label={property.name!}
                        parentPath={parentPath}
                        property={property}
                        sampleOutput={sampleOutput}
                        workflowNodeName={workflowNodeName}
                    />

                    {property.properties && !!property.properties.length && (
                        <div
                            className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                            key={property.name}
                        >
                            <SchemaProperties
                                parentPath={path}
                                properties={property.properties}
                                sampleOutput={sampleOutput}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    )}

                    {property.items && !!property.items.length && (
                        <div
                            className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                            key={property.name}
                        >
                            <SchemaProperties
                                parentPath={path}
                                properties={property.items}
                                sampleOutput={sampleOutput}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    )}
                </li>
            );
        })}
    </ul>
);

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
                                            <AnimateSpin />
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
                                    Upload Sample Output
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>

                    <div className="mt-2 flex items-center">
                        <div className="group relative flex items-center rounded-md p-1 pr-5 hover:bg-gray-100">
                            <span title={outputSchema.type}>
                                {TYPE_ICONS[outputSchema.type as keyof typeof TYPE_ICONS]}
                            </span>

                            <span className="ml-2 pr-2 text-sm text-gray-800">{currentNode.name}</span>

                            {sampleOutput && typeof sampleOutput !== 'object' && (
                                <div className="flex-1 text-xs text-muted-foreground">{String(sampleOutput)}</div>
                            )}

                            <div className="absolute right-0.5">
                                <ClipboardIcon
                                    aria-hidden="true"
                                    className="invisible size-4 cursor-pointer bg-background text-gray-400 group-hover:visible"
                                    onClick={() => copyToClipboard(`$\{${currentNode.name}}`)}
                                />
                            </div>
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

                                {!uploadSampleOutputRequestMutation.isPending && <>Upload Sample Output</>}
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
