import SubflowIcon from '@/assets/subflow.svg';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {CornerDownRightIcon, InfoIcon, PlusIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {Control, FieldValues} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

import {ComponentConnection, Workflow} from '../middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '../queries/platform/componentDefinitions.queries';
import EnvironmentBadge from './EnvironmentBadge';

interface ConnectionRenderItemI {
    connection: ComponentConnection;
    groupedIndices?: number[];
    index: number;
}

export interface SubflowDuplicateStubI {
    subflowWorkflowUuid: string;
    subflowWorkflowUuidPath: string[];
}

interface SubflowTreeNodeI {
    children: Map<string, SubflowTreeNodeI>;
    connections: ConnectionRenderItemI[];
    duplicateStubs: SubflowDuplicateStubI[];
    subflowWorkflowUuid: string;
}

interface ConnectionConfigurationListFormFieldProps {
    componentConnection: ComponentConnection;
    connectionDialogAllowed: boolean;
    connections: ConnectionI[];
    control: Control<FieldValues>;
    currentConnectionId?: number;
    fieldNamePrefix: string;
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
    fieldNamePrefix,
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

    const currentConnection = connectionList.find((connection) => connection.id === currentConnectionId);

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
            name={`${fieldNamePrefix}.${index}.connectionId`}
            render={({field}) => (
                <FormItem>
                    <FormLabel className="flex items-center gap-1">
                        {componentDefinition?.icon && (
                            <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                        )}

                        <span className="max-w-[70%] truncate">
                            {workflowNodeLabel || `${componentDefinition?.title} Connection`}
                        </span>

                        {groupedIndices ? (
                            <span className="text-xs text-content-neutral-secondary">
                                (applies to {groupedIndices.length} nodes)
                            </span>
                        ) : (
                            <span className="text-xs text-content-neutral-secondary">
                                {`(${componentConnection.componentName} - ${componentConnection.workflowNodeName})`}
                            </span>
                        )}
                    </FormLabel>

                    <Select
                        defaultValue={currentConnectionId != null ? currentConnectionId.toString() : undefined}
                        name={field.name}
                        onValueChange={handleConnectionValueChange}
                        value={
                            (currentConnectionId != null
                                ? currentConnectionId.toString()
                                : field.value
                                  ? field.value.toString()
                                  : undefined) || ''
                        }
                    >
                        <FormControl>
                            <div className="flex min-w-0 space-x-2 bg-surface-neutral-primary">
                                <SelectTrigger className="min-w-0 overflow-hidden text-left shadow-none [&>span]:block [&>span]:min-w-0 [&>span]:flex-1 [&>span]:overflow-hidden">
                                    {currentConnectionId ? (
                                        <SelectValue placeholder="Select a connection...">
                                            {currentConnection && (
                                                <div className="flex w-full min-w-0 items-center space-x-1">
                                                    <span className="min-w-0 truncate">{currentConnection.name}</span>

                                                    <span className="shrink-0">
                                                        <EnvironmentBadge
                                                            environmentId={currentConnection.environmentId!}
                                                        />
                                                    </span>
                                                </div>
                                            )}
                                        </SelectValue>
                                    ) : (
                                        <SelectValue placeholder="Select a connection...">
                                            <span className="text-content-neutral-secondary/80">
                                                Select a connection...
                                            </span>
                                        </SelectValue>
                                    )}
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

                        <SelectContent className="max-w-(--radix-select-trigger-width)">
                            <SelectItem value="null">Select a connection...</SelectItem>

                            {connectionList.map((connection) => (
                                <SelectItem
                                    className="w-full [&>span[id]]:flex [&>span[id]]:w-full [&>span[id]]:min-w-0 [&>span[id]]:items-center [&>span[id]]:overflow-hidden"
                                    key={connection.id}
                                    value={connection.id!.toString()}
                                >
                                    <span className="mr-1 min-w-0 flex-1 truncate">{connection.name}</span>

                                    {!!connection?.tags?.length && (
                                        <span className="mr-1 min-w-0 flex-none truncate text-xs text-content-neutral-secondary">
                                            {connection.tags?.map((tag) => tag.name).join(', ')}
                                        </span>
                                    )}

                                    <span className="flex-none">
                                        <EnvironmentBadge environmentId={connection.environmentId!} />
                                    </span>
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    {connectionList.length === 0 && (
                        <div className="flex items-center gap-1">
                            <InfoIcon className="size-4 text-content-destructive-primary" />

                            <span className="text-sm text-content-destructive-primary">No connections available</span>
                        </div>
                    )}

                    <FormMessage />
                </FormItem>
            )}
            rules={{
                required: componentConnection.required,
            }}
        />
    );
};

const countSubflowNodeConnections = (node: SubflowTreeNodeI): number => {
    let count = node.connections.length;

    for (const childNode of node.children.values()) {
        count += countSubflowNodeConnections(childNode);
    }

    return count;
};

interface InheritedSubflowConnectionStubProps {
    stub: SubflowDuplicateStubI;
    subflowLabelMap?: Map<string, string>;
}

const InheritedSubflowConnectionStub = ({stub, subflowLabelMap}: InheritedSubflowConnectionStubProps) => (
    <div className="flex items-center gap-2 rounded-md border bg-surface-neutral-primary px-3 py-2.5">
        <InlineSVG className="size-5" src={SubflowIcon} />

        <span className="text-sm font-medium text-content-neutral-primary">
            {subflowLabelMap?.get(stub.subflowWorkflowUuid) || 'Subflow connections'}
        </span>

        <Badge className="ml-auto flex gap-1 font-semibold uppercase" styleType="secondary-filled">
            <CornerDownRightIcon className="size-3" />
            Inherited
        </Badge>
    </div>
);

interface SubflowConnectionGroupProps {
    connectionDialogAllowed: boolean;
    connections?: ConnectionI[];
    connectionsGrouped: boolean;
    control: Control<FieldValues>;
    fieldNamePrefix: string;
    getCurrentConnectionId?: (index: number) => number | undefined;
    handleConnectionIdChange: (index: number, connectionId: number) => void;
    handleConnectionDialogOpen?: (componentConnection: ComponentConnection) => void;
    node: SubflowTreeNodeI;
    subflowLabelMap?: Map<string, string>;
    workflowNodeLabelMap: Map<string, string>;
}

const SubflowConnectionGroup = ({
    connectionDialogAllowed,
    connections,
    connectionsGrouped,
    control,
    fieldNamePrefix,
    getCurrentConnectionId,
    handleConnectionDialogOpen,
    handleConnectionIdChange,
    node,
    subflowLabelMap,
    workflowNodeLabelMap,
}: SubflowConnectionGroupProps) => {
    const connectionCount = countSubflowNodeConnections(node);

    return (
        <Collapsible
            className="group/subflow space-y-4 rounded-md border bg-surface-neutral-primary px-3 py-2.5 transition-all has-[>button:focus-visible]:ring-2 has-[>button:focus-visible]:ring-stroke-brand-focus data-[state=open]:p-3"
            defaultOpen
        >
            <CollapsibleTrigger className="group/trigger flex w-full items-center justify-between outline-hidden">
                <div className="flex gap-2">
                    <InlineSVG className="size-5" src={SubflowIcon} />

                    <span className="text-sm font-medium text-content-neutral-primary underline-offset-2 group-hover/trigger:underline">
                        {subflowLabelMap?.get(node.subflowWorkflowUuid) || 'Subflow connections'}
                    </span>

                    <span className="text-sm font-light text-content-neutral-primary">
                        ({connectionCount > 1 ? `${connectionCount} connections` : '1 connection'})
                    </span>
                </div>

                <CaretDownIcon className="size-4 text-content-neutral-secondary transition-all group-data-[state=open]/subflow:rotate-180" />
            </CollapsibleTrigger>

            <CollapsibleContent className="flex flex-col gap-4">
                {node.connections.map((connectionItem) => (
                    <ConnectionConfigurationListFormField
                        componentConnection={connectionItem.connection}
                        connectionDialogAllowed={connectionDialogAllowed}
                        connections={connections!}
                        control={control}
                        currentConnectionId={getCurrentConnectionId?.(connectionItem.index)}
                        fieldNamePrefix={fieldNamePrefix}
                        handleConnectionDialogOpen={handleConnectionDialogOpen}
                        handleConnectionIdChange={handleConnectionIdChange}
                        index={connectionItem.index}
                        key={`${connectionItem.connection.workflowNodeName}_${connectionItem.connection.key}_${connectionsGrouped}`}
                        workflowNodeLabel={workflowNodeLabelMap.get(connectionItem.connection.workflowNodeName)}
                    />
                ))}

                {Array.from(node.children.values()).map((childNode) => (
                    <SubflowConnectionGroup
                        connectionDialogAllowed={connectionDialogAllowed}
                        connections={connections}
                        connectionsGrouped={connectionsGrouped}
                        control={control}
                        fieldNamePrefix={fieldNamePrefix}
                        getCurrentConnectionId={getCurrentConnectionId}
                        handleConnectionDialogOpen={handleConnectionDialogOpen}
                        handleConnectionIdChange={handleConnectionIdChange}
                        key={childNode.subflowWorkflowUuid}
                        node={childNode}
                        subflowLabelMap={subflowLabelMap}
                        workflowNodeLabelMap={workflowNodeLabelMap}
                    />
                ))}

                {node.duplicateStubs.map((stub) => (
                    <InheritedSubflowConnectionStub
                        key={stub.subflowWorkflowUuidPath.join('/')}
                        stub={stub}
                        subflowLabelMap={subflowLabelMap}
                    />
                ))}
            </CollapsibleContent>
        </Collapsible>
    );
};

interface ConnectionConfigurationListProps {
    componentConnections: ComponentConnection[];
    connectionDialogAllowed?: boolean;
    connections?: ConnectionI[];
    connectionsGrouped?: boolean;
    control: Control<FieldValues>;
    duplicateSubflowStubs?: SubflowDuplicateStubI[];
    fieldNamePrefix?: string;
    getCurrentConnectionId?: (index: number) => number | undefined;
    handleConnectionIdChange: (index: number, connectionId: number) => void;
    handleConnectionDialogOpen?: (componentConnection: ComponentConnection) => void;
    subflowLabelMap?: Map<string, string>;
    workflow: Workflow;
}

const ConnectionConfigurationList = ({
    componentConnections,
    connectionDialogAllowed = false,
    connections,
    connectionsGrouped = false,
    control,
    duplicateSubflowStubs,
    fieldNamePrefix = 'connections',
    getCurrentConnectionId,
    handleConnectionDialogOpen,
    handleConnectionIdChange,
    subflowLabelMap,
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

    const {regularConnections, subflowConnectionTree, topLevelStubs} = useMemo(() => {
        if (connectionsGrouped) {
            return {
                regularConnections: connectionsToRender,
                subflowConnectionTree: new Map<string, SubflowTreeNodeI>(),
                topLevelStubs: [] as SubflowDuplicateStubI[],
            };
        }

        const regularConnections: typeof connectionsToRender = [];
        const subflowConnectionTree = new Map<string, SubflowTreeNodeI>();

        const createSubflowNode = (uuid: string): SubflowTreeNodeI => ({
            children: new Map<string, SubflowTreeNodeI>(),
            connections: [],
            duplicateStubs: [],
            subflowWorkflowUuid: uuid,
        });

        for (const item of connectionsToRender) {
            const uuidPath = item.connection.subflowWorkflowUuidPath ?? [];

            if (!uuidPath.length) {
                regularConnections.push(item);

                continue;
            }

            let currentLevel = subflowConnectionTree;
            let targetNode: SubflowTreeNodeI | undefined;

            for (const uuid of uuidPath) {
                let node = currentLevel.get(uuid);

                if (!node) {
                    node = createSubflowNode(uuid);

                    currentLevel.set(uuid, node);
                }

                targetNode = node;
                currentLevel = node.children;
            }

            targetNode!.connections.push(item);
        }

        // Attach each duplicate subflow stub under its parent node, creating any intermediate nodes
        // that hold no real connections of their own. A stub whose parent path is empty is a
        // top-level duplicate and is rendered alongside the root groups.
        const topLevelStubs: SubflowDuplicateStubI[] = [];

        for (const stub of duplicateSubflowStubs ?? []) {
            const parentPath = stub.subflowWorkflowUuidPath.slice(0, -1);

            if (!parentPath.length) {
                topLevelStubs.push(stub);

                continue;
            }

            let currentLevel = subflowConnectionTree;
            let parentNode: SubflowTreeNodeI | undefined;

            for (const uuid of parentPath) {
                let node = currentLevel.get(uuid);

                if (!node) {
                    node = createSubflowNode(uuid);

                    currentLevel.set(uuid, node);
                }

                parentNode = node;
                currentLevel = node.children;
            }

            parentNode!.duplicateStubs.push(stub);
        }

        return {regularConnections, subflowConnectionTree, topLevelStubs};
    }, [connectionsToRender, connectionsGrouped, duplicateSubflowStubs]);

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

    if (!connectionsToRender.length && !(duplicateSubflowStubs?.length && !connectionsGrouped)) {
        return <h3 className="p-4 text-center font-medium text-content-neutral-primary">No Connections yet</h3>;
    }

    return (
        <div className="space-y-4">
            {regularConnections.map(({connection, groupedIndices, index}) => (
                <ConnectionConfigurationListFormField
                    componentConnection={connection}
                    connectionDialogAllowed={connectionDialogAllowed}
                    connections={connections!}
                    control={control}
                    currentConnectionId={getCurrentConnectionId?.(index)}
                    fieldNamePrefix={fieldNamePrefix}
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

            {Array.from(subflowConnectionTree.values()).map((node) => (
                <SubflowConnectionGroup
                    connectionDialogAllowed={connectionDialogAllowed}
                    connections={connections}
                    connectionsGrouped={connectionsGrouped}
                    control={control}
                    fieldNamePrefix={fieldNamePrefix}
                    getCurrentConnectionId={getCurrentConnectionId}
                    handleConnectionDialogOpen={handleConnectionDialogOpen}
                    handleConnectionIdChange={handleConnectionIdChange}
                    key={node.subflowWorkflowUuid}
                    node={node}
                    subflowLabelMap={subflowLabelMap}
                    workflowNodeLabelMap={workflowNodeLabelMap}
                />
            ))}

            {topLevelStubs.map((stub) => (
                <InheritedSubflowConnectionStub
                    key={stub.subflowWorkflowUuidPath.join('/')}
                    stub={stub}
                    subflowLabelMap={subflowLabelMap}
                />
            ))}
        </div>
    );
};

export default ConnectionConfigurationList;
