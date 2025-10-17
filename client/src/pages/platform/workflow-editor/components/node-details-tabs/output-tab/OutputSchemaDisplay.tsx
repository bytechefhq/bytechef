import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import PropertyField from '@/pages/platform/workflow-editor/components/PropertyField';
import SchemaProperties from '@/pages/platform/workflow-editor/components/SchemaProperties';
import {PropertyAllType} from '@/shared/types';
import {ChevronDownIcon, PenIcon} from 'lucide-react';

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

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button disabled={saveWorkflowNodeTestOutputMutation.isPending} size="sm" variant="outline">
                            <PenIcon /> Define <ChevronDownIcon className="ml-0.5" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="w-60 cursor-pointer">
                        {!variablePropertiesDefined && (
                            <DropdownMenuItem
                                className="cursor-pointer"
                                disabled={connectionMissing}
                                onClick={handleTestOperationClick}
                            >
                                {`Test ${currentNode.trigger ? 'Trigger' : 'Action'}`}
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuItem className="cursor-pointer" onClick={() => setShowUploadDialog(true)}>
                            Upload Sample Output Data
                        </DropdownMenuItem>

                        <DropdownMenuItem className="cursor-pointer" onClick={handlePredefinedOutputSchemaClick}>
                            Reset
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
