import RequiredMark from '@/components/RequiredMark';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ConnectionI, useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

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

    const {connectionDialogAllowed, currentComponent, currentNode, setCurrentComponent, setCurrentNode} =
        useWorkflowNodeDetailsPanelStore();

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    if (workflowTestConfigurationConnection) {
        if (connectionId !== workflowTestConfigurationConnection.connectionId) {
            setConnectionId(workflowTestConfigurationConnection.connectionId);
        }
    }

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

    const handleValueChange = useCallback(
        (connectionId: number, workflowConnectionKey: string, rootClusterElementNodeDataWorkflowNodeName?: string) => {
            if (!connectionId) {
                return;
            }

            saveWorkflowTestConfigurationConnectionMutation.mutate({
                saveWorkflowTestConfigurationConnectionRequest: {
                    connectionId,
                },
                workflowConnectionKey,
                workflowId,
                workflowNodeName: rootClusterElementNodeDataWorkflowNodeName || workflowNodeName,
            });

            setConnectionId(connectionId);

            if (currentNode) {
                if (rootClusterElementNodeData && currentNode.clusterElementType) {
                    const rootConnectionIds = Array.isArray(rootClusterElementNodeData.connectionId)
                        ? [...rootClusterElementNodeData.connectionId]
                        : [];

                    if (currentNode.connectionId !== undefined && typeof currentNode.connectionId === 'number') {
                        const connectionToReplace = rootConnectionIds.indexOf(currentNode.connectionId);

                        if (connectionToReplace > -1) {
                            rootConnectionIds.splice(connectionToReplace, 1);
                        }
                    }

                    if (!rootConnectionIds.includes(connectionId)) {
                        rootConnectionIds.push(connectionId);
                    }

                    setRootClusterElementNodeData({
                        ...rootClusterElementNodeData,
                        connectionId: rootConnectionIds,
                    });

                    setCurrentNode({
                        ...currentNode,
                        connectionId,
                    });
                } else {
                    setCurrentNode({
                        ...currentNode,
                        connectionId,
                    });
                }
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
            currentNode,
            queryClient,
            rootClusterElementNodeData,
            saveWorkflowTestConfigurationConnectionMutation,
            setCurrentComponent,
            setCurrentNode,
            setRootClusterElementNodeData,
            workflowId,
            workflowNodeName,
        ]
    );

    useEffect(() => {
        if (workflowTestConfigurationConnection && connectionId !== workflowTestConfigurationConnection.connectionId) {
            setConnectionId(workflowTestConfigurationConnection.connectionId);
        }
    }, [workflowTestConfigurationConnection, connectionId]);

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
                    onValueChange={(value) => {
                        handleValueChange(+value, key, rootClusterElementNodeData?.workflowNodeName);
                    }}
                    required={required}
                    value={connectionId ? connectionId.toString() : undefined}
                >
                    <div className="flex space-x-2">
                        {componentConnections && componentConnections.length > 0 && (
                            <SelectTrigger>
                                <SelectValue placeholder="Choose Connection..." />
                            </SelectTrigger>
                        )}

                        {componentDefinition && connectionDialogAllowed && (
                            <Button
                                className={twMerge('mt-auto p-2', !componentConnections?.length && 'w-full')}
                                onClick={() => setShowConnectionDialog(true)}
                                title="Create a new connection"
                                variant="outline"
                            >
                                <PlusIcon className="size-5" />

                                {!componentConnections?.length && 'Create Connection'}
                            </Button>
                        )}

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

                                        <Badge variant="outline">{connection.environment}</Badge>
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
                    connectionDefinition={connectionDefinition}
                    connectionParameters={currentConnection.connectionParameters}
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
                        handleValueChange(newConnectionId, key, rootClusterElementNodeData?.workflowNodeName);

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
