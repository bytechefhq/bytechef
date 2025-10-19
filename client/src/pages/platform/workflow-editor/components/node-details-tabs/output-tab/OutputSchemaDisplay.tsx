import {Button} from '@/components/ui/button';
import {ButtonGroup} from '@/components/ui/button-group';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import PropertyField from '@/pages/platform/workflow-editor/components/PropertyField';
import SchemaProperties from '@/pages/platform/workflow-editor/components/SchemaProperties';
import {PropertyAllType} from '@/shared/types';
import {MoreHorizontalIcon} from 'lucide-react';

interface OutputSchemaDisplayProps {
    connectionMissing: boolean;
    copiedValue: string | null;
    copyToClipboard: (value: string) => Promise<void>;
    currentNode: {name: string; trigger?: boolean; action?: boolean};
    handlePredefinedOutputSchemaClick: () => void;
    handleTestOperationClick: () => void;
    outputSchema: PropertyAllType;
    sampleOutput?: object;
    saveWorkflowNodeTestOutputMutation: {isPending: boolean};
    setShowUploadDialog: (show: boolean) => void;
    variablePropertiesDefined?: boolean;
}

const OutputSchemaDisplay = ({
    connectionMissing,
    copiedValue = null,
    copyToClipboard,
    currentNode,
    handlePredefinedOutputSchemaClick,
    handleTestOperationClick,
    outputSchema,
    sampleOutput,
    saveWorkflowNodeTestOutputMutation,
    setShowUploadDialog,
    variablePropertiesDefined,
}: OutputSchemaDisplayProps) => {
    const hasProperties = Boolean(outputSchema && 'properties' in outputSchema && outputSchema.properties);
    const hasItems = Boolean(outputSchema && 'items' in outputSchema && outputSchema.items);

    return (
        <div className="h-full">
            <div className="mb-2 flex items-center justify-between">
                <h3 className="text-sm text-gray-500">Output Schema</h3>

                <ButtonGroup>
                    {!variablePropertiesDefined && (
                        <Button
                            disabled={connectionMissing || saveWorkflowNodeTestOutputMutation.isPending}
                            onClick={handleTestOperationClick}
                            variant="outline"
                        >
                            {`Test ${currentNode.trigger ? 'Trigger' : 'Action'}`}
                        </Button>
                    )}

                    {variablePropertiesDefined && (
                        <Button
                            disabled={saveWorkflowNodeTestOutputMutation.isPending}
                            onClick={() => setShowUploadDialog(true)}
                            variant="outline"
                        >
                            Upload Sample Output
                        </Button>
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label="More Options"
                                asChild
                                disabled={saveWorkflowNodeTestOutputMutation.isPending}
                                size="icon"
                                variant="outline"
                            >
                                <span>
                                    <MoreHorizontalIcon />
                                </span>
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end" className="w-52">
                            <DropdownMenuGroup>
                                {!variablePropertiesDefined && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => setShowUploadDialog(true)}
                                    >
                                        Upload Sample Output
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem
                                    className="cursor-pointer"
                                    onClick={handlePredefinedOutputSchemaClick}
                                >
                                    Reset
                                </DropdownMenuItem>
                            </DropdownMenuGroup>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </ButtonGroup>
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

            {hasProperties && sampleOutput && (
                <SchemaProperties
                    copiedValue={copiedValue}
                    copyToClipboard={copyToClipboard}
                    properties={(outputSchema as PropertyAllType).properties!}
                    sampleOutput={sampleOutput}
                    workflowNodeName={currentNode.name}
                />
            )}

            {hasItems && sampleOutput && (
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
    );
};

export default OutputSchemaDisplay;
