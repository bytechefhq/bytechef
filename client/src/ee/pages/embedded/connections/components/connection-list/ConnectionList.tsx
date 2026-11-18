import ConnectedUserConnectionListItem from '@/ee/pages/embedded/connections/components/connection-list/ConnectedUserConnectionListItem';
import ConnectionOriginBadge from '@/ee/pages/embedded/connections/components/connection-list/ConnectionOriginBadge';
import {Connection, ConnectionOriginEnum, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

import ConnectionListItem from './ConnectionListItem';

interface ConnectionListProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connectedUserId?: number;
    connectedUserIntegrationInstances?: ConnectedUserIntegrationInstance[];
    connections: Connection[];
    tags: Tag[];
}

const ConnectionList = ({
    componentDefinitions,
    connectedUserId,
    connectedUserIntegrationInstances,
    connections,
    tags,
}: ConnectionListProps) => {
    const provisionedIntegrationInstancesByConnectionId = useMemo(
        () =>
            new Map(
                (connectedUserIntegrationInstances ?? [])
                    .filter((integrationInstance) => integrationInstance.provisioned)
                    .map((integrationInstance) => [integrationInstance.connectionId, integrationInstance])
            ),
        [connectedUserIntegrationInstances]
    );

    return (
        <ul className="w-full divide-y divide-border/50 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5" role="list">
            {connections.map((connection) => {
                if (connectedUserId !== undefined && connection.origin !== ConnectionOriginEnum.Internal) {
                    return (
                        <ConnectedUserConnectionListItem
                            componentDefinitions={componentDefinitions}
                            connectedUserId={connectedUserId}
                            connection={connection}
                            key={connection.id}
                            provisionedIntegrationInstance={provisionedIntegrationInstancesByConnectionId.get(
                                connection.id
                            )}
                        />
                    );
                }

                const connectionTagIds = connection.tags?.map((tag) => tag.id);

                return (
                    <ConnectionListItem
                        badges={
                            connectedUserId !== undefined ? (
                                <ConnectionOriginBadge origin={connection.origin} />
                            ) : undefined
                        }
                        componentDefinitions={componentDefinitions}
                        connection={connection}
                        key={connection.id}
                        remainingTags={tags?.filter((tag) => !connectionTagIds?.includes(tag.id))}
                    />
                );
            })}
        </ul>
    );
};

export default ConnectionList;
