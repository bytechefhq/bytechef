import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import useDataSyncElementStep from '@/pages/automation/data-syncs/hooks/useDataSyncElementStep';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {DataSync, DataSyncElementKind} from '@/shared/middleware/graphql';

interface DataSyncElementStepProps {
    dataSync: DataSync;
    kind: DataSyncElementKind;
}

const COPY: Record<DataSyncElementKind, {description: string; title: string}> = {
    [DataSyncElementKind.Destination]: {
        description: 'Choose where the records go and configure its connection and parameters.',
        title: 'Select Destination',
    },
    [DataSyncElementKind.Processor]: {description: '', title: ''},
    [DataSyncElementKind.Source]: {
        description: 'Choose a data source component and configure its connection and parameters.',
        title: 'Select Source',
    },
};

export default function DataSyncElementStep({dataSync, kind}: DataSyncElementStepProps) {
    const {
        candidateDefinitions,
        connectionRequired,
        connections,
        element,
        form,
        formDisplayConditions,
        handleComponentChange,
        handleConnectionChange,
        handleOperationChange,
        hasConnection,
        operations,
        properties,
        selectedComponentName,
        selectedOperationName,
    } = useDataSyncElementStep({dataSync, kind});

    const idPrefix = kind.toLowerCase();
    const formValues = form.watch();

    return (
        <div className="space-y-6 py-4">
            <div>
                <h2 className="text-lg font-semibold">{COPY[kind].title}</h2>

                <p className="mt-1 text-sm text-muted-foreground">{COPY[kind].description}</p>
            </div>

            <fieldset className="flex flex-col gap-2 border-0 p-0">
                <label className="text-sm font-medium" htmlFor={`${idPrefix}-component-select`}>
                    Component
                </label>

                <Select onValueChange={handleComponentChange} value={selectedComponentName}>
                    <SelectTrigger id={`${idPrefix}-component-select`}>
                        <SelectValue placeholder="Select a component..." />
                    </SelectTrigger>

                    <SelectContent>
                        {candidateDefinitions.map((definition) => (
                            <SelectItem key={definition.name} value={definition.name}>
                                {definition.title || definition.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </fieldset>

            {selectedComponentName && operations.length > 0 && (
                <fieldset className="flex flex-col gap-2 border-0 p-0">
                    <label className="text-sm font-medium" htmlFor={`${idPrefix}-operation-select`}>
                        Operation
                    </label>

                    <Select onValueChange={handleOperationChange} value={selectedOperationName}>
                        <SelectTrigger id={`${idPrefix}-operation-select`}>
                            <SelectValue placeholder="Select an operation..." />
                        </SelectTrigger>

                        <SelectContent>
                            {operations.map((operation) => (
                                <SelectItem key={operation.name} value={operation.name}>
                                    {operation.title || operation.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>
            )}

            {element && element.componentName === selectedComponentName && hasConnection && (
                <fieldset className="flex flex-col gap-2 border-0 p-0">
                    <label className="text-sm font-medium" htmlFor={`${idPrefix}-connection-select`}>
                        Connection
                    </label>

                    <Select onValueChange={handleConnectionChange} value={formValues.connectionId ?? ''}>
                        <SelectTrigger id={`${idPrefix}-connection-select`}>
                            <SelectValue placeholder="Select a connection..." />
                        </SelectTrigger>

                        <SelectContent>
                            {connections.map((connection) => (
                                <SelectItem key={connection.id} value={String(connection.id)}>
                                    {connection.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>
            )}

            {element && properties.length > 0 && (
                <WorkflowMockProvider>
                    <ClusterElementProvider
                        value={{
                            clusterElementName: element.operationName,
                            componentName: element.componentName,
                            componentVersion: element.componentVersion,
                            connectionId: formValues.connectionId != null ? Number(formValues.connectionId) : undefined,
                            connectionRequired,
                            inputParameters: formValues.parameters ?? {},
                        }}
                    >
                        <Properties
                            control={form.control}
                            controlPath="parameters"
                            customClassName="p-0"
                            formDisplayConditions={formDisplayConditions}
                            formState={form.formState}
                            properties={properties}
                        />
                    </ClusterElementProvider>
                </WorkflowMockProvider>
            )}
        </div>
    );
}
