import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import ConnectedUserSheetPanelConnectionListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelConnectionListItem';
import ConnectedUserSheetPanelListSkeleton from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelListSkeleton';
import {EMBEDDED_CONNECTIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetAllConnectedUserConnectionsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import {Link2Icon} from 'lucide-react';
import {useMemo} from 'react';

interface ConnectedUserSheetPanelConnectionListProps {
    connectedUserId: number;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
}

const ConnectedUserSheetPanelConnectionList = ({
    connectedUserId,
    connectedUserIntegrationInstances,
}: ConnectedUserSheetPanelConnectionListProps) => {
    const {data: connections, isError, isLoading, refetch} = useGetAllConnectedUserConnectionsQuery(connectedUserId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentDefinitionsByName = useMemo(
        () =>
            new Map(
                (componentDefinitions ?? []).map((componentDefinition) => [
                    componentDefinition.name,
                    componentDefinition,
                ])
            ),
        [componentDefinitions]
    );

    const provisionedIntegrationInstancesByConnectionId = useMemo(
        () =>
            new Map(
                connectedUserIntegrationInstances
                    .filter((integrationInstance) => integrationInstance.provisioned)
                    .map((integrationInstance) => [integrationInstance.connectionId, integrationInstance])
            ),
        [connectedUserIntegrationInstances]
    );

    if (isLoading) {
        return <ConnectedUserSheetPanelListSkeleton label="Loading connections" />;
    }

    if (isError) {
        return (
            <div className="flex flex-col items-start gap-2 py-4">
                <p className="text-sm text-destructive">Connections could not be loaded.</p>

                <Button label="Retry" onClick={() => refetch()} variant="outline" />
            </div>
        );
    }

    if (!connections || connections.length === 0) {
        return (
            <EmptyState
                description="Connections this user creates, connections provisioned for them and shared admin connections show up here."
                docsLink={{
                    href: EMBEDDED_CONNECTIONS_DOCUMENTATION_URL,
                    label: 'How connections work',
                }}
                icon={<Link2Icon />}
                title="No connections yet"
            />
        );
    }

    return (
        <ul>
            {connections.map((connection) => (
                <ConnectedUserSheetPanelConnectionListItem
                    componentTitle={componentDefinitionsByName.get(connection.componentName)?.title}
                    connectedUserId={connectedUserId}
                    connection={connection}
                    icon={componentDefinitionsByName.get(connection.componentName)?.icon}
                    key={connection.id}
                    provisionedIntegrationInstance={provisionedIntegrationInstancesByConnectionId.get(connection.id)}
                />
            ))}
        </ul>
    );
};

export default ConnectedUserSheetPanelConnectionList;
