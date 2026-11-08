import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useAddAiHubUserConnectorMutation} from '@/shared/middleware/graphql';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';

interface AiHubConnectConnectionDialogProps {
    componentName: string;
    componentVersion: number;
    onClose: () => void;
}

const AiHubConnectConnectionDialog = ({
    componentName,
    componentVersion,
    onClose,
}: AiHubConnectConnectionDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [showCreate, setShowCreate] = useState(false);

    const queryClient = useQueryClient();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({componentName, componentVersion});

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const connectionTagsQueryResult = useGetConnectionTagsQuery();

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {componentName, environmentId: currentEnvironmentId, id: currentWorkspaceId ?? 0},
        currentWorkspaceId != null
    );

    const addMutation = useAddAiHubUserConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']});

            onClose();
        },
    });

    const linkConnection = (connectionId: number) => {
        addMutation.mutate({
            componentName,
            componentVersion,
            connectionId: String(connectionId),
            environment: currentEnvironmentId,
            workspaceId: String(currentWorkspaceId ?? ''),
        });
    };

    if (showCreate) {
        if (!componentDefinitions || !componentDefinition || currentWorkspaceId == null) {
            return null;
        }

        return (
            <ConnectionDialog
                componentDefinition={componentDefinition}
                componentDefinitions={componentDefinitions}
                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                connectionsQueryKey={ConnectionKeys.connections}
                onClose={onClose}
                onConnectionCreate={(newConnectionId) => {
                    queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

                    linkConnection(newConnectionId);
                }}
                useCreateConnectionMutation={useCreateConnectionMutation}
                useGetConnectionTagsQuery={() => connectionTagsQueryResult}
            />
        );
    }

    const existingConnections = connections ?? [];

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Reuse an existing connection or create a new one."
                        title={`Connect ${componentDefinition?.title ?? componentName}`}
                    />

                    <DialogBody className="flex flex-col">
                        {existingConnections.length > 0 && (
                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-medium text-muted-foreground">Existing connections</span>

                                <div className="flex max-h-60 flex-col gap-1 overflow-y-auto">
                                    {existingConnections.map((connection) => (
                                        <div
                                            className="flex items-center gap-2 rounded-md border border-border px-3 py-2"
                                            key={connection.id}
                                        >
                                            <span className="min-w-0 flex-1 truncate text-sm">{connection.name}</span>

                                            <Button
                                                label="Use"
                                                onClick={() => connection.id != null && linkConnection(connection.id)}
                                                size="sm"
                                                variant="outline"
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <Button
                            icon={<PlusIcon />}
                            label="Create new connection"
                            onClick={() => setShowCreate(true)}
                            variant="outline"
                        />
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubConnectConnectionDialog;
