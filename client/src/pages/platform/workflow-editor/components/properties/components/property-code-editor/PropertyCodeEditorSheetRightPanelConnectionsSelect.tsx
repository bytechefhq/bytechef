import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';

export interface PropertyCodeEditorSheetRightPanelConnectionsSelectProps {
    componentConnection: ComponentConnection;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
}
const PropertyCodeEditorSheetRightPanelConnectionsSelect = ({
    componentConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: PropertyCodeEditorSheetRightPanelConnectionsSelectProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnection) {
        connectionId = workflowTestConfigurationConnection.connectionId;
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
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
            environmentId: currentEnvironmentId,
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
                onValueChange={(value) => handleValueChange(+value, componentConnection.key)}
                required={componentConnection.required}
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

                                    <EnvironmentBadge environmentId={connection.environmentId!} />
                                </div>
                            </SelectItem>
                        ))}
                </SelectContent>
            </Select>

            {showNewConnectionDialog && componentDefinitions && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    componentDefinitions={componentDefinitions}
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
