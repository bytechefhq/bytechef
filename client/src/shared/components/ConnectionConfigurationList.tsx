import Button from '@/components/Button/Button';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {PlusIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {Control, FieldValues} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

import {ComponentConnection, Workflow} from '../middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '../queries/platform/componentDefinitions.queries';
import EnvironmentBadge from './EnvironmentBadge';

interface ConnectionConfigurationListFormFieldProps {
    componentConnection: ComponentConnection;
    connectionDialogAllowed: boolean;
    connections: ConnectionI[];
    control: Control<FieldValues>;
    currentConnectionId?: number;
    groupedIndices?: number[];
    index: number;
    handleConnectionIdChange: (index: number, connectionId: number) => void;
    handleConnectionDialogOpen?: (componentConnection: ComponentConnection) => void;
    workflowNodeLabel?: string;
}

const ConnectionConfigurationListFormField = ({
    componentConnection,
    connectionDialogAllowed,
    connections,
    control,
    currentConnectionId,
    groupedIndices,
    handleConnectionDialogOpen,
    handleConnectionIdChange,
    index,
    workflowNodeLabel,
}: ConnectionConfigurationListFormFieldProps) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    const handleConnectionValueChange = useCallback(
        (value: string) => {
            const connectionId = Number(value);

            handleConnectionIdChange(index, connectionId);

            if (groupedIndices) {
                for (const groupedIndex of groupedIndices) {
                    if (groupedIndex !== index) {
                        handleConnectionIdChange(groupedIndex, connectionId);
                    }
                }
            }
        },
        [groupedIndices, index, handleConnectionIdChange]
    );

    const connectionList = connections.filter(
        (connection) => connection.componentName === componentConnection.componentName
    );

    const openConnectionDialog = () => {
        if (!handleConnectionDialogOpen) {
            return;
        }

        handleConnectionDialogOpen(componentConnection);
    };

    return (
        <FormField
            control={control}
            key={index}
            name={`connections.${index}.connectionId`}
            render={({field}) => (
                <FormItem>
                    <FormLabel className="flex items-center">
                        {componentDefinition?.icon && (
                            <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                        )}

                        <span className="ml-1">{workflowNodeLabel || `${componentDefinition?.title} Connection`}</span>

                        {groupedIndices ? (
                            <span className="ml-0.5 text-xs text-content-neutral-secondary">
                                (applies to {groupedIndices.length} nodes)
                            </span>
                        ) : (
                            <span className="ml-0.5 text-xs text-content-neutral-secondary">
                                {`(${componentConnection.workflowNodeName} - ${componentConnection.key})`}
                            </span>
                        )}
                    </FormLabel>

                    <Select
                        onValueChange={handleConnectionValueChange}
                        value={
                            currentConnectionId != null
                                ? currentConnectionId.toString()
                                : field.value
                                  ? field.value.toString()
                                  : undefined
                        }
                    >
                        <FormControl className="flex space-x-2">
                            <div className="flex space-x-2">
                                <SelectTrigger>
                                    <SelectValue placeholder="Choose Connection..." />
                                </SelectTrigger>

                                {connectionDialogAllowed && (
                                    <Button
                                        className="mt-auto p-2"
                                        icon={<PlusIcon className="size-5" />}
                                        onClick={openConnectionDialog}
                                        title="Create a new connection"
                                        type="button"
                                        variant="outline"
                                    />
                                )}
                            </div>
                        </FormControl>

                        <SelectContent>
                            {connectionList.map((connection) => (
                                <SelectItem
                                    className="flex items-center"
                                    key={connection.id}
                                    value={connection.id!.toString()}
                                >
                                    <span className="mr-1">{connection.name}</span>

                                    <span className="text-xs text-content-neutral-secondary">
                                        {connection?.tags?.map((tag) => tag.name).join(', ')}
                                    </span>

                                    <EnvironmentBadge environmentId={connection.environmentId!} />
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    <FormMessage />
                </FormItem>
            )}
            rules={{
                required: componentConnection.required,
            }}
        />
    );
};

interface ConnectionConfigurationListProps {
    componentConnections: ComponentConnection[];
    connectionDialogAllowed?: boolean;
    connections: ConnectionI[];
    connectionsGrouped?: boolean;
    control: Control<FieldValues>;
    getCurrentConnectionId?: (index: number) => number | undefined;
    handleConnectionIdChange: (index: number, connectionId: number) => void;
    handleConnectionDialogOpen?: (componentConnection: ComponentConnection) => void;
    workflow: Workflow;
}

const ConnectionConfigurationList = ({
    componentConnections,
    connectionDialogAllowed = false,
    connections,
    connectionsGrouped = false,
    control,
    getCurrentConnectionId,
    handleConnectionDialogOpen,
    handleConnectionIdChange,
    workflow,
}: ConnectionConfigurationListProps) => {
    const connectionsToRender = useMemo((): Array<{
        connection: ComponentConnection;
        groupedIndices?: number[];
        index: number;
    }> => {
        if (!connectionsGrouped) {
            const ungroupedConnections = componentConnections.map((connection, index) => ({connection, index}));

            return ungroupedConnections;
        }

        const connectionGroupMap = new Map<string, number[]>();

        for (const [index, connection] of componentConnections.entries()) {
            const componentName = connection.componentName;
            const existingIndices = connectionGroupMap.get(componentName);

            if (existingIndices) {
                existingIndices.push(index);
            } else {
                connectionGroupMap.set(componentName, [index]);
            }
        }

        const groupedConnections = Array.from(connectionGroupMap.values()).map((indices) => ({
            connection: componentConnections[indices[0]],
            groupedIndices: indices.length > 1 ? indices : undefined,
            index: indices[0],
        }));

        return groupedConnections;
    }, [componentConnections, connectionsGrouped]);

    const workflowNodeLabelMap = useMemo(() => {
        if (connectionsGrouped) {
            return new Map<string, string>();
        }

        const workflowNodeLabelMap = new Map<string, string>();

        for (const task of workflow?.tasks ?? []) {
            if (task.label) {
                workflowNodeLabelMap.set(task.name, task.label);
            }
        }

        for (const trigger of workflow?.triggers ?? []) {
            if (trigger.label) {
                workflowNodeLabelMap.set(trigger.name, trigger.label);
            }
        }

        return workflowNodeLabelMap;
    }, [connectionsGrouped, workflow?.tasks, workflow?.triggers]);

    return (
        <div className="space-y-4">
            {connectionsToRender.map(({connection, groupedIndices, index}) => (
                <ConnectionConfigurationListFormField
                    componentConnection={connection}
                    connectionDialogAllowed={connectionDialogAllowed}
                    connections={connections}
                    control={control}
                    currentConnectionId={getCurrentConnectionId?.(index)}
                    groupedIndices={groupedIndices}
                    handleConnectionDialogOpen={handleConnectionDialogOpen}
                    handleConnectionIdChange={handleConnectionIdChange}
                    index={index}
                    key={`${connection.workflowNodeName}_${connection.key}_${connectionsGrouped}`}
                    workflowNodeLabel={
                        groupedIndices ? undefined : workflowNodeLabelMap.get(connection.workflowNodeName)
                    }
                />
            ))}
        </div>
    );
};

export default ConnectionConfigurationList;
