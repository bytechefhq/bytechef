import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ConnectionI, useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {
    useDeleteWorkflowTestConfigurationConnectionMutation,
    useSaveWorkflowTestConfigurationConnectionMutation,
} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, X} from 'lucide-react';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorStore from '../../../stores/useWorkflowEditorStore';

type ConnectionTabConnectionSelectPropsType = {
    componentConnection: ComponentConnection;
    componentConnectionsCount: number;
    componentDefinition: ComponentDefinition;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
};

const ConnectionTabConnectionSelect = ({
    componentConnection,
    componentConnectionsCount,
    componentDefinition,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: ConnectionTabConnectionSelectPropsType) => {
    const [connectionId, setConnectionId] = useState<number | undefined>();
    const [currentConnection, setCurrentConnection] = useState<ConnectionI>();
    const [showConnectionDialog, setShowConnectionDialog] = useState<boolean>(false);

    const connectionIdRef = useRef<number | undefined>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {connectionDialogAllowed, currentComponent, currentNode, setCurrentComponent, setCurrentNode} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                connectionDialogAllowed: state.connectionDialogAllowed,
                currentComponent: state.currentComponent,
                currentNode: state.currentNode,
                setCurrentComponent: state.setCurrentComponent,
                setCurrentNode: state.setCurrentNode,
            }))
        );

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    const {rootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
        }))
    );

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {componentName, componentVersion, key, required} = componentConnection;

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({
        componentName,
        componentVersion,
    });

    const {data: componentConnections} = useGetConnectionsQuery!(
        {
            componentName: componentConnection?.componentName,
            connectionVersion: componentConnection?.componentVersion,
        },
        !!componentDefinition
    );

    const queryClient = useQueryClient();

    const saveWorkflowTestConfigurationConnectionMutation = useSaveWorkflowTestConfigurationConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
    });

    const deleteWorkflowTestConfigurationConnectionMutation = useDeleteWorkflowTestConfigurationConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
    });

    const handleValueChange = useCallback(
        (connectionId: number, workflowConnectionKey: string) => {
            if (!connectionId) {
                return;
            }

            saveWorkflowTestConfigurationConnectionMutation.mutate({
                environmentId: currentEnvironmentId,
                saveWorkflowTestConfigurationConnectionRequest: {
                    connectionId,
                },
                workflowConnectionKey,
                workflowId,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || workflowNodeName,
            });

            setConnectionId(connectionId);

            connectionIdRef.current = connectionId;

            if (currentNode) {
                setCurrentNode({...currentNode, connectionId});
            }

            if (currentComponent) {
                setCurrentComponent({...currentComponent, connectionId});
            }

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties, workflowId],
            });

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, workflowId],
            });

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeOptionKeys.clusterElementNodeOptions, workflowId],
            });
        },
        [
            currentComponent,
            currentEnvironmentId,
            currentNode,
            queryClient,
            rootClusterElementNodeData?.workflowNodeName,
            saveWorkflowTestConfigurationConnectionMutation,
            setCurrentComponent,
            setCurrentNode,
            workflowId,
            workflowNodeName,
        ]
    );

    const skipServerSyncRef = useRef(false);
    const clearedConnectionIdRef = useRef<number | undefined>(undefined);

    const handleClear = useCallback(
        (workflowConnectionKey: string) => {
            const prevConnectionId = connectionId ?? 0;

            skipServerSyncRef.current = true;
            clearedConnectionIdRef.current = connectionId;

            deleteWorkflowTestConfigurationConnectionMutation.mutate({
                environmentId: currentEnvironmentId,
                saveWorkflowTestConfigurationConnectionRequest: {connectionId: prevConnectionId},
                workflowConnectionKey,
                workflowId,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || workflowNodeName,
            });

            setConnectionId(undefined);

            if (currentNode) {
                setCurrentNode({...currentNode, connectionId: undefined});
            }

            if (currentComponent) {
                setCurrentComponent({...currentComponent, connectionId: undefined});
            }

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties, workflowId],
            });
            queryClient.removeQueries({
                queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, workflowId],
            });
            queryClient.removeQueries({
                queryKey: [...WorkflowNodeOptionKeys.clusterElementNodeOptions, workflowId],
            });
        },
        [
            connectionId,
            currentComponent,
            currentEnvironmentId,
            currentNode,
            deleteWorkflowTestConfigurationConnectionMutation,
            queryClient,
            rootClusterElementNodeData?.workflowNodeName,
            setCurrentComponent,
            setCurrentNode,
            workflowId,
            workflowNodeName,
        ]
    );

    // Sync connectionId from prop to state (one-way sync)
    useEffect(() => {
        const serverConnectionId = workflowTestConfigurationConnection?.connectionId;

        if (
            skipServerSyncRef.current &&
            serverConnectionId !== undefined &&
            serverConnectionId === clearedConnectionIdRef.current
        ) {
            return;
        }

        if (skipServerSyncRef.current && serverConnectionId === undefined) {
            skipServerSyncRef.current = false;
            clearedConnectionIdRef.current = undefined;
        }

        if (serverConnectionId !== undefined && connectionId !== serverConnectionId) {
            setConnectionId(serverConnectionId);
        }
        if (serverConnectionId === undefined && connectionId !== undefined) {
            setConnectionId(undefined);
        }
    }, [workflowTestConfigurationConnection, connectionId]);

    // Update connectionId ref when state changes (for the sync effect above)
    useEffect(() => {
        connectionIdRef.current = connectionId;
    }, [connectionId]);

    useEffect(() => {
        const newComponentConnection = componentConnections?.find((connection) => connection.id === connectionId);

        setCurrentConnection(newComponentConnection);
    }, [componentConnections, connectionId]);

    return (
        <div className="flex flex-col gap-6">
            <div className="space-y-1">
                <div className="flex items-center gap-1">
                    {componentDefinition && (
                        <Label className="text-sm font-medium">
                            {componentDefinition.title}

                            {required && <RequiredMark />}
                        </Label>
                    )}

                    {componentConnectionsCount > 1 && <Label className="text-sm text-muted-foreground">{key}</Label>}
                </div>

                <Select
                    key={connectionId !== undefined ? `conn-${connectionId}` : 'conn-none'}
                    onValueChange={(value) => handleValueChange(+value, key)}
                    required={required}
                    value={connectionId !== undefined ? connectionId.toString() : undefined}
                >
                    <div className="flex space-x-2 bg-white">
                        {componentConnections && componentConnections.length > 0 && (
                            <SelectTrigger>
                                <SelectValue placeholder="Choose Connection..." />
                            </SelectTrigger>
                        )}

                        {connectionId !== undefined && (
                            <Button
                                icon={<X />}
                                onClick={() => handleClear(key)}
                                size="icon"
                                title="Clear connection"
                                variant="outline"
                            />
                        )}

                        {componentDefinition &&
                            connectionDialogAllowed &&
                            (componentConnections?.length ? (
                                <Button
                                    icon={<PlusIcon />}
                                    onClick={() => setShowConnectionDialog(true)}
                                    size="icon"
                                    title="Create a new connection"
                                    variant="outline"
                                />
                            ) : (
                                <Button
                                    className="w-full"
                                    icon={<PlusIcon />}
                                    label="Create Connection"
                                    onClick={() => setShowConnectionDialog(true)}
                                    title="Create a new connection"
                                    variant="outline"
                                />
                            ))}

                        {!connectionDialogAllowed && !componentConnections?.length && <p>No connections available.</p>}
                    </div>

                    <SelectContent>
                        {componentConnections &&
                            componentConnections.map((connection) => (
                                <SelectItem key={connection.id} value={connection.id!.toString()}>
                                    <div className="flex items-center space-x-1">
                                        <span>{connection.name}</span>

                                        <span className="text-xs text-gray-500">
                                            {connection?.tags?.map((tag) => tag.name).join(', ')}
                                        </span>

                                        <EnvironmentBadge environmentId={+connection.environmentId!} />
                                    </div>
                                </SelectItem>
                            ))}
                    </SelectContent>
                </Select>
            </div>

            {currentConnection && connectionDefinition && (
                <ConnectionParameters
                    authorizationParameters={currentConnection.authorizationParameters}
                    authorizationType={currentConnection.authorizationType}
                    baseUri={currentConnection.baseUri}
                    connectionDefinition={connectionDefinition}
                    connectionParameters={currentConnection.connectionParameters}
                    customAction={componentDefinition?.actions?.some((action) => action.name === 'customAction')}
                />
            )}

            {showConnectionDialog && componentDefinitions && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    componentDefinitions={componentDefinitions}
                    connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                    connectionsQueryKey={ConnectionKeys!.connections}
                    onClose={() => setShowConnectionDialog(false)}
                    onConnectionCreate={(newConnectionId) => {
                        handleValueChange(newConnectionId, key);

                        setConnectionId(newConnectionId);
                    }}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                />
            )}
        </div>
    );
};

export default ConnectionTabConnectionSelect;
