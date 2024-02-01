import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useCreateConnectionMutation, useUpdateConnectionMutation} from '@/mutations/automation/connections.mutations';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/mutations/platform/workflowTestConfigurations.mutations';
import {
    WorkflowTestConfigurationKeys,
    useGetWorkflowTestConfigurationConnectionsQuery,
} from '@/queries/platform/workflowTestConfigurations.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import EmptyList from 'components/EmptyList';
import {LinkIcon, PlusIcon} from 'lucide-react';
import {ComponentDefinitionModel} from 'middleware/platform/configuration';
import ConnectionDialog from 'pages/platform/connection/components/ConnectionDialog';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from 'queries/automation/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

const ConnectionTab = ({
    componentDefinition,
    workflowConnectionKey,
    workflowId,
    workflowNodeName,
}: {
    componentDefinition: ComponentDefinitionModel;
    workflowConnectionKey: string;
    workflowNodeName: string;
    workflowId: string;
}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentDefinition.name,
            connectionVersion: componentDefinition.connection?.version,
        },
        !!componentDefinition.connection?.componentName
    );

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        workflowId,
        workflowNodeName,
    });

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnections && workflowTestConfigurationConnections.length > 0) {
        connectionId = workflowTestConfigurationConnections[0].connectionId;
    }

    const queryClient = useQueryClient();

    const saveWorkflowTestConfigurationConnectionMutation = useSaveWorkflowTestConfigurationConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
    });

    const handleValueChange = (connectionId: number) => {
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
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            {connections?.length ? (
                <Select
                    onValueChange={(value) => handleValueChange(+value)}
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

                    <p className="text-sm text-gray-800">The selected connection is used for testing purposes only.</p>
                </div>
            )}

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
        </div>
    );
};

export default ConnectionTab;
