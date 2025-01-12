import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import {useConnectionQuery} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import {WorkflowConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

export interface PropertyCodeEditorSheetRightPanelConnectionsSelectProps {
    workflowConnection: WorkflowConnection;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
}
const PropertyCodeEditorSheetRightPanelConnectionsSelect = ({
    workflowConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: PropertyCodeEditorSheetRightPanelConnectionsSelectProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery, useGetConnectionsQuery} =
        useConnectionQuery();

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnection) {
        connectionId = workflowTestConfigurationConnection.connectionId;
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: connections} = useGetConnectionsQuery!(
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
            saveWorkflowTestConfigurationConnectionRequest: {
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
                        onClick={() => setShowNewConnectionDialog(true)}
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
                                    <span className="mr-1">{connection.name}</span>

                                    <span className="text-xs text-gray-500">
                                        {connection?.tags?.map((tag) => tag.name).join(', ')}
                                    </span>

                                    <Badge variant="outline">{connection.environment}</Badge>
                                </div>
                            </SelectItem>
                        ))}
                </SelectContent>
            </Select>

            {showNewConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                    connectionsQueryKey={ConnectionKeys!.connections}
                    onClose={() => setShowNewConnectionDialog(false)}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                />
            )}
        </>
    );
};

export default PropertyCodeEditorSheetRightPanelConnectionsSelect;
