import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ConnectionParameters from '@/pages/platform/connection/components/ConnectionParameters';
import {useConnectionQuery} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import {
    ComponentDefinitionModel,
    WorkflowConnectionModel,
    WorkflowTestConfigurationConnectionModel,
} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import EmptyList from 'components/EmptyList';
import {LinkIcon, PlusIcon} from 'lucide-react';
import ConnectionDialog from 'pages/platform/connection/components/ConnectionDialog';
import {useEffect, useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';

const ConnectionLabel = ({
    workflowConnection,
    workflowConnectionsCount,
}: {
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionsCount: number;
}) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    return (
        <div className="space-x-1">
            {componentDefinition && (
                <Label>
                    {`${componentDefinition.title}`}

                    {workflowConnection.required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                </Label>
            )}

            {workflowConnectionsCount > 1 && (
                <Label className="text-sm text-muted-foreground">{workflowConnection.key}</Label>
            )}
        </div>
    );
};

const ConnectionSelect = ({
    workflowConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: {
    workflowConnection: WorkflowConnectionModel;
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
    };

    useEffect(() => {
        if (currentNode) {
            setCurrentNode({...currentNode, connectionId});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [connectionId]);

    const connection = connections?.find((connection) => connection.id === connectionId);

    return (
        <div className="flex flex-col gap-4">
            <Select
                onValueChange={(value) => handleValueChange(+value, workflowConnection.key)}
                required={workflowConnection.required}
                value={connectionId ? connectionId.toString() : undefined}
            >
                <div className="flex space-x-2">
                    <SelectTrigger>
                        <SelectValue placeholder="Choose Connection..." />
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

            {connection && connectionDefinition && (
                <ConnectionParameters connection={connection} connectionDefinition={connectionDefinition} />
            )}
        </div>
    );
};

const ConnectionTab = ({
    componentDefinition,
    workflowConnections,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: {
    componentDefinition: ComponentDefinitionModel;
    workflowConnections: WorkflowConnectionModel[];
    workflowNodeName: string;
    workflowId: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnectionModel>;
}) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery} = useConnectionQuery();

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            {workflowConnections?.length ? (
                workflowConnections.map((workflowConnection) => {
                    const workflowTestConfigurationConnection =
                        workflowTestConfigurationConnections && workflowTestConfigurationConnections.length > 0
                            ? workflowTestConfigurationConnections.filter(
                                  (workflowTestConfigurationConnection) =>
                                      workflowTestConfigurationConnection.workflowConnectionKey ===
                                      workflowConnection.key
                              )[0]
                            : undefined;

                    return (
                        <fieldset className="space-y-2" key={workflowConnection.key}>
                            <ConnectionLabel
                                workflowConnection={workflowConnection}
                                workflowConnectionsCount={workflowConnections.length}
                            />

                            <ConnectionSelect
                                workflowConnection={workflowConnection}
                                workflowId={workflowId}
                                workflowNodeName={workflowNodeName}
                                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                            />
                        </fieldset>
                    );
                })
            ) : (
                <EmptyList
                    button={
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
                            connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                            connectionsQueryKey={ConnectionKeys!.connections}
                            triggerNode={<Button title="Create a new connection">Create a connection</Button>}
                            useCreateConnectionMutation={useCreateConnectionMutation}
                            useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                        />
                    }
                    icon={<LinkIcon className="size-6 text-gray-400" />}
                    message="You have not created any connections for this component yet."
                    title="No Connections"
                />
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex items-center pb-2">
                        <span className="font-medium">Note</span>

                        <button
                            className="ml-auto p-0"
                            onClick={() => setShowConnectionNote(false)}
                            title="Close the note"
                        >
                            <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </div>

                    <p className="text-sm text-gray-800">
                        The selected connections are used for testing purposes only.
                    </p>
                </div>
            )}
        </div>
    );
};

export default ConnectionTab;
