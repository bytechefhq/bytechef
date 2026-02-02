import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {PlusIcon} from 'lucide-react';

import usePropertyCodeEditorDialogRightPanelConnectionsSelect from './hooks/usePropertyCodeEditorDialogRightPanelConnectionsSelect';

export interface PropertyCodeEditorDialogRightPanelConnectionsSelectProps {
    componentConnection: ComponentConnection;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
}

const PropertyCodeEditorDialogRightPanelConnectionsSelect = ({
    componentConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: PropertyCodeEditorDialogRightPanelConnectionsSelectProps) => {
    const {
        ConnectionKeys,
        componentDefinition,
        componentDefinitions,
        connectionId,
        connections,
        handleValueChange,
        setShowNewConnectionDialog,
        showNewConnectionDialog,
        useCreateConnectionMutation,
        useGetConnectionTagsQuery,
    } = usePropertyCodeEditorDialogRightPanelConnectionsSelect({
        componentConnection,
        workflowId,
        workflowNodeName,
        workflowTestConfigurationConnection,
    });

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
                        className="mt-auto"
                        icon={<PlusIcon />}
                        onClick={() => setShowNewConnectionDialog(true)}
                        size="icon"
                        title="Create a new connection"
                        variant="outline"
                    />
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

export default PropertyCodeEditorDialogRightPanelConnectionsSelect;
