import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import useClusterElementStep from '@/pages/platform/cluster-element-editor/data-stream-editor/hooks/useClusterElementStep';
import ConnectionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';

export default function DataStreamDestinationStep() {
    const {
        componentConnections,
        displayConditionsQuery,
        elementComponentDefinition,
        elementItem,
        elementProperties,
        handleComponentChange,
        handleOperationChange,
        rootWorkflowNodeName,
        selectedComponentName,
        selectedOperationName,
        stepComponentDefinitions,
        stepOperations,
        testConnections,
        workflowId,
    } = useClusterElementStep('destination');

    return (
        <div className="space-y-4 py-4">
            <div>
                <h2 className="text-lg font-semibold">Select Destination</h2>

                <p className="text-sm text-muted-foreground">
                    Choose a data destination component and configure its connection and parameters.
                </p>
            </div>

            {rootWorkflowNodeName && (
                <>
                    <fieldset className="flex flex-col gap-2 border-0 p-0">
                        <label className="text-sm font-medium" htmlFor="destination-component-select">
                            Component
                        </label>

                        <Select onValueChange={handleComponentChange} value={selectedComponentName}>
                            <SelectTrigger id="destination-component-select">
                                <SelectValue placeholder="Select a destination component..." />
                            </SelectTrigger>

                            <SelectContent>
                                {stepComponentDefinitions.map((definition) => (
                                    <SelectItem key={definition.name} value={definition.name}>
                                        {definition.title || definition.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {selectedComponentName && stepOperations.length > 0 && (
                        <fieldset className="flex flex-col gap-2 border-0 p-0">
                            <label className="text-sm font-medium" htmlFor="destination-operation-select">
                                Operation
                            </label>

                            <Select onValueChange={handleOperationChange} value={selectedOperationName}>
                                <SelectTrigger id="destination-operation-select">
                                    <SelectValue placeholder="Select an operation..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {stepOperations.map((operation) => (
                                        <SelectItem key={operation.name} value={operation.name}>
                                            {operation.title || operation.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>
                    )}

                    {elementItem && elementComponentDefinition?.connection && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Connection</h3>

                            <ConnectionTab
                                componentConnections={componentConnections}
                                currentComponentDefinition={elementComponentDefinition}
                                workflowId={workflowId!}
                                workflowNodeName={elementItem.name}
                                workflowTestConfigurationConnections={testConnections}
                            />
                        </div>
                    )}

                    {elementItem && elementProperties.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Properties</h3>

                            <Properties
                                customClassName="p-0"
                                displayConditionsQuery={displayConditionsQuery}
                                operationName={elementItem.operationName}
                                properties={elementProperties}
                            />
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
