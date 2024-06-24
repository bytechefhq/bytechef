import {Button} from '@/components/ui/button';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import ConnectionParameters from '@/pages/platform/connection/components/ConnectionParameters';
import {useConnectionQuery} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    WorkflowConnectionModel,
    WorkflowTestConfigurationConnectionModel,
} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

const ConnectionTabConnectionSelect = ({
    workflowConnection,
    workflowConnectionsCount,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: {
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionsCount: number;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnectionModel;
}) => {
    const [connectionId, setConnectionId] = useState<number | undefined>();

    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery, useGetConnectionsQuery} =
        useConnectionQuery();

    if (workflowTestConfigurationConnection) {
        if (connectionId !== workflowTestConfigurationConnection.connectionId) {
            setConnectionId(workflowTestConfigurationConnection.connectionId);
        }
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    const {data: connections} = useGetConnectionsQuery!(
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

    const handleValueChange = (connectionId: number, workflowConnectionKey: string) => {
        saveWorkflowTestConfigurationConnectionMutation.mutate({
            saveWorkflowTestConfigurationConnectionRequestModel: {
                connectionId,
            },
            workflowConnectionKey,
            workflowId,
            workflowNodeName,
        });

        setConnectionId(connectionId);

        queryClient.removeQueries({
            queryKey: [...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties, workflowId],
        });

        queryClient.removeQueries({
            queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, workflowId],
        });
    };

    useEffect(() => {
        if (currentNode) {
            setCurrentNode({...currentNode, connectionId});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [connectionId]);

    const connection = connections?.find((connection) => connection.id === connectionId);

    return (
        <div className="flex flex-col gap-6">
            <SelectGroup>
                {componentDefinition && (
                    <SelectLabel className="font-normal">
                        {componentDefinition.title}

                        {workflowConnection.required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                    </SelectLabel>
                )}

                {workflowConnectionsCount > 1 && (
                    <SelectLabel className="text-muted-foreground text-sm">{workflowConnection.key}</SelectLabel>
                )}

                <Select
                    onValueChange={(value) => handleValueChange(+value, workflowConnection.key)}
                    required={workflowConnection.required}
                    value={connectionId ? connectionId.toString() : undefined}
                >
                    <div className="flex space-x-2">
                        <SelectTrigger>
                            <SelectValue placeholder={`${connections?.length ? 'Choose' : 'Create'} a Connection...`} />
                        </SelectTrigger>

                        {componentDefinition && (
                            <ConnectionDialog
                                componentDefinition={componentDefinition}
                                connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                                connectionsQueryKey={ConnectionKeys!.connections}
                                triggerNode={
                                    <Button className="mt-auto p-2" title="Create a new connection" variant="outline">
                                        <PlusIcon className="size-5" />
                                    </Button>
                                }
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                            />
                        )}
                    </div>

                    <SelectContent>
                        {connections &&
                            connections.map((connection) => (
                                <SelectItem key={connection.id} value={connection.id!.toString()}>
                                    <div className="flex items-center">
                                        <span className="mr-1 ">{connection.name}</span>

                                        <span className="text-xs text-gray-500">
                                            {connection?.tags?.map((tag) => tag.name).join(', ')}
                                        </span>
                                    </div>
                                </SelectItem>
                            ))}
                    </SelectContent>
                </Select>

                {currentConnection && connectionDefinition && (
                    <ConnectionParameters connection={currentConnection} connectionDefinition={connectionDefinition} />
                )}
            </SelectGroup>
        </div>
    );
};

export default ConnectionTabConnectionSelect;
