import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import ConnectionScopeBadge from '@/pages/automation/connections/components/ConnectionScopeBadge';
import {ConnectionI, useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/middleware/graphql';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {useDeleteWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';
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

    const clearedConnectionIdRef = useRef<number | undefined>(undefined);
    const connectionIdRef = useRef<number | undefined>(undefined);
    const skipServerSyncRef = useRef(false);

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
            connectionVersion: connectionDefinition?.version,
        },
        !!componentDefinition && !!connectionDefinition
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

            const previousConnectionId = connectionIdRef.current;

            setConnectionId(connectionId);

            connectionIdRef.current = connectionId;

            saveWorkflowTestConfigurationConnectionMutation.mutate(
                {
                    connectionId,
                    environmentId: currentEnvironmentId,
                    workflowConnectionKey,
                    workflowId,
                    workflowNodeName: rootClusterElementNodeData?.workflowNodeName || workflowNodeName,
                },
                {
                    onError: () => {
                        if (connectionIdRef.current !== connectionId) {
                            return;
                        }

                        setConnectionId(previousConnectionId);

                        connectionIdRef.current = previousConnectionId;

                        toast.error('Failed to save connection');
                    },
                    onSuccess: () => {
                        if (connectionIdRef.current !== connectionId) {
                            return;
                        }

                        const latestState = useWorkflowNodeDetailsPanelStore.getState();
                        const latestNode = latestState.currentNode;
                        const latestComponent = latestState.currentComponent;

                        if (latestNode) {
                            latestState.setCurrentNode({...latestNode, connectionId});
                        }

                        if (latestComponent) {
                            latestState.setCurrentComponent({...latestComponent, connectionId});
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
                }
            );
        },
        [
            currentEnvironmentId,
            queryClient,
            rootClusterElementNodeData?.workflowNodeName,
            saveWorkflowTestConfigurationConnectionMutation,
            workflowId,
            workflowNodeName,
        ]
    );

    const handleClearConnectionClick = useCallback(
        (workflowConnectionKey: string) => {
            const previousConnectionId = connectionId ?? 0;

            skipServerSyncRef.current = true;
            clearedConnectionIdRef.current = connectionId;

            deleteWorkflowTestConfigurationConnectionMutation.mutate({
                deleteWorkflowTestConfigurationConnectionRequest: {connectionId: previousConnectionId},
                environmentId: currentEnvironmentId,
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

    const handleOnConnectionCreate = useCallback(
        async (newConnectionId: number) => {
            await queryClient.invalidateQueries({
                queryKey: ConnectionKeys!.connections,
            });

            handleValueChange(newConnectionId, key);

            setConnectionId(newConnectionId);
        },
        [ConnectionKeys, handleValueChange, key, queryClient]
    );

    const groupedConnections = useMemo(() => {
        const visibilityOrder: Array<'ORGANIZATION' | 'PRIVATE' | 'PROJECT' | 'WORKSPACE'> = [
            'PRIVATE',
            'PROJECT',
            'WORKSPACE',
            'ORGANIZATION',
        ];

        const groupLabels: Record<string, string> = {
            ORGANIZATION: 'Organization',
            PRIVATE: 'Private',
            PROJECT: 'Project',
            WORKSPACE: 'Workspace',
        };

        const groups = visibilityOrder
            .map((visibility) => ({
                connections: (componentConnections ?? []).filter(
                    (connection) => (connection.visibility || 'PRIVATE') === visibility
                ),
                label: groupLabels[visibility],
                visibility,
            }))
            .filter((group) => group.connections.length > 0);

        return groups;
    }, [componentConnections]);

    useEffect(() => {
        const workflowConnectionId = workflowTestConfigurationConnection?.connectionId;

        // skipServerSyncRef suppresses this effect after a local clear, so the server echo of the
        // old value (still arriving inflight) does not re-populate the selector and undo the user's
        // action. The ref is reset once the server confirms the cleared state.
        if (
            skipServerSyncRef.current &&
            workflowConnectionId !== undefined &&
            workflowConnectionId === clearedConnectionIdRef.current
        ) {
            return;
        }

        if (skipServerSyncRef.current && workflowConnectionId === undefined) {
            skipServerSyncRef.current = false;
            clearedConnectionIdRef.current = undefined;
        }

        if (workflowConnectionId !== undefined && connectionId !== workflowConnectionId) {
            setConnectionId(workflowConnectionId);
        }
        if (workflowConnectionId === undefined && connectionId !== undefined) {
            setConnectionId(undefined);
        }
    }, [workflowTestConfigurationConnection, connectionId]);

    useEffect(() => {
        connectionIdRef.current = connectionId;
    }, [connectionId]);

    useEffect(() => {
        const newComponentConnection = componentConnections?.find((connection) => connection.id === connectionId);

        setCurrentConnection(newComponentConnection);
    }, [componentConnections, connectionId]);

    return (
        <div className="flex min-w-0 flex-col gap-6">
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
                    <div className="flex w-full min-w-0 space-x-2">
                        {componentConnections && componentConnections.length > 0 && (
                            <div className="min-w-0 flex-1 bg-content-onsurface-primary">
                                <SelectTrigger>
                                    <SelectValue placeholder="Choose Connection..." />
                                </SelectTrigger>
                            </div>
                        )}

                        {connectionId !== undefined && (
                            <Button
                                icon={<XIcon />}
                                onClick={() => handleClearConnectionClick(key)}
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
                        {groupedConnections.length === 0 && (
                            <div className="px-3 py-2 text-xs text-muted-foreground">
                                <p>No connections found for this component in the current environment.</p>

                                <p className="mt-1">
                                    Connections in other environments or with a different connection version are not
                                    shown.
                                </p>
                            </div>
                        )}

                        {groupedConnections.map((group) => (
                            <SelectGroup key={group.visibility}>
                                <SelectLabel className="text-xs font-semibold uppercase text-muted-foreground">
                                    {group.label}
                                </SelectLabel>

                                {group.connections.map((connection) => (
                                    <SelectItem key={connection.id} value={connection.id!.toString()}>
                                        <div className="flex items-center space-x-1">
                                            <span>{connection.name}</span>

                                            <span className="text-xs text-gray-500">
                                                {connection?.tags?.map((tag) => tag.name).join(', ')}
                                            </span>

                                            <EnvironmentBadge environmentId={+connection.environmentId!} />

                                            {connection.visibility && (
                                                <ConnectionScopeBadge visibility={connection.visibility} />
                                            )}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectGroup>
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
                    onConnectionCreate={handleOnConnectionCreate}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                />
            )}
        </div>
    );
};

export default ConnectionTabConnectionSelect;
