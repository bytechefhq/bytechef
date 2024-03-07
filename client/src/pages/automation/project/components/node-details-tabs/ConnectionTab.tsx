import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useCreateConnectionMutation, useUpdateConnectionMutation} from '@/mutations/automation/connections.mutations';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {
    WorkflowTestConfigurationKeys,
    useGetWorkflowTestConfigurationConnectionsQuery,
} from '@/queries/platform/workflowTestConfigurations.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import EmptyList from 'components/EmptyList';
import {LinkIcon, PlusIcon} from 'lucide-react';
import {
    ComponentDefinitionModel,
    WorkflowConnectionModel,
    WorkflowTestConfigurationConnectionModel,
} from 'middleware/platform/configuration';
import ConnectionDialog from 'pages/platform/connection/components/ConnectionDialog';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from 'queries/automation/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

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
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnection) {
        connectionId = workflowTestConfigurationConnection.connectionId;
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentDefinition?.name!,
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
    };

    return (
        <>
            <Select
                onValueChange={(value) => handleValueChange(+value, workflowConnection.key)}
                required={workflowConnection.required}
                value={connectionId ? connectionId.toString() : undefined}
            >
                <div className="flex space-x-2">
                    <SelectTrigger>
                        <SelectValue placeholder="Choose Connection..." />
                    </SelectTrigger>

                    <Button
                        className="mt-auto p-2"
                        onClick={() => setShowEditConnectionDialog(true)}
                        title="Create a new connection"
                        variant="outline"
                    >
                        <PlusIcon className="size-5" />
                    </Button>
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

            {showEditConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </>
    );
};

const ConnectionTab = ({
    componentDefinition,
    workflowConnections,
    workflowId,
    workflowNodeName,
}: {
    componentDefinition: ComponentDefinitionModel;
    workflowConnections: WorkflowConnectionModel[];
    workflowNodeName: string;
    workflowId: string;
}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        workflowId,
        workflowNodeName,
    });

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            {workflowConnections?.length ? (
                workflowConnections.map((workflowConnection) => (
                    <fieldset className="space-y-2" key={workflowConnection.key}>
                        <ConnectionLabel
                            workflowConnection={workflowConnection}
                            workflowConnectionsCount={workflowConnections.length}
                        />

                        <ConnectionSelect
                            workflowConnection={workflowConnection}
                            workflowId={workflowId}
                            workflowNodeName={workflowNodeName}
                            workflowTestConfigurationConnection={
                                workflowTestConfigurationConnections && workflowTestConfigurationConnections.length > 0
                                    ? workflowTestConfigurationConnections.filter(
                                          (workflowTestConfigurationConnection) =>
                                              workflowTestConfigurationConnection.workflowConnectionKey ===
                                              workflowConnection.key
                                      )[0]
                                    : undefined
                            }
                        />
                    </fieldset>
                ))
            ) : (
                <EmptyList
                    button={
                        <Button onClick={() => setShowEditConnectionDialog(true)} title="Create a new connection">
                            Create a connection
                        </Button>
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

            {showEditConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={workflowConnections.length === 1 ? componentDefinition : undefined}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </div>
    );
};

export default ConnectionTab;
