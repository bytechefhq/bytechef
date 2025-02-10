import RequiredMark from '@/components/RequiredMark';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {ConnectionI, useConnectionQuery} from '@/shared/components/connection/providers/connectionReactQueryProvider';
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

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery, useGetConnectionsQuery} =
        useConnectionQuery();

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
            componentName: componentDefinition?.name,
            connectionVersion: componentDefinition?.connection?.version,
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
        (connectionId: number, workflowConnectionKey: string) => {
            if (!connectionId) {
                return;
            }

            saveWorkflowTestConfigurationConnectionMutation.mutate({
                saveWorkflowTestConfigurationConnectionRequest: {
                    connectionId,
                },
                workflowConnectionKey,
                workflowId,
                workflowNodeName,
            });

            setConnectionId(connectionId);

            const newComponentConnection = componentConnections?.find((connection) => connection.id === connectionId);

            if (currentNode) {
                setCurrentNode({...currentNode, connectionId});
            }

            if (currentComponent) {
                setCurrentComponent({...currentComponent, connectionId});
            }

            setCurrentConnection(newComponentConnection);

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties, workflowId],
            });

            queryClient.removeQueries({
                queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, workflowId],
            });
        },
        [
            componentConnections,
            currentComponent,
            currentNode,
            queryClient,
            saveWorkflowTestConfigurationConnectionMutation,
            setCurrentComponent,
            setCurrentNode,
            workflowId,
            workflowNodeName,
        ]
    );

    useEffect(() => {
        if (workflowTestConfigurationConnection && connectionId !== workflowTestConfigurationConnection.connectionId) {
            setConnectionId(workflowTestConfigurationConnection.connectionId);
        }
    }, [workflowTestConfigurationConnection, connectionId]);

    return (
        <div className="flex flex-col gap-6">
            <div className="space-y-1">
                {componentDefinition && (
                    <Label className="mb-2 font-normal">
                        {componentDefinition.title}

                        {required && <RequiredMark />}
                    </Label>
                )}

                {componentConnectionsCount > 1 && <Label className="text-sm text-muted-foreground">{key}</Label>}

                <Select
                    onValueChange={(value) => handleValueChange(+value, key)}
                    required={required}
                    value={connectionId ? connectionId.toString() : undefined}
                >
                    <div className="flex space-x-2">
                        {componentConnections && componentConnections.length > 0 && (
                            <SelectTrigger>
                                <SelectValue placeholder="Choose Connection..." />
                            </SelectTrigger>
                        )}

                        {componentDefinition && (
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
                    connectionDefinition={connectionDefinition}
                    connectionParameters={currentConnection.connectionParameters}
                />
            )}

            {showConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                    connectionsQueryKey={ConnectionKeys!.connections}
                    onClose={() => setShowConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                />
            )}
        </div>
    );
};

export default ConnectionTabConnectionSelect;
