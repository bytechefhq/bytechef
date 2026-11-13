import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {
    getUpdateConnectedUserConnectionCredentials,
    useUpdateConnectionMutation,
} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useMemo} from 'react';

interface ConnectedUserConnectionCredentialsDialogProps {
    connectedUserId: number;
    connection: Connection;
    onClose: () => void;
}

const ConnectedUserConnectionCredentialsDialog = ({
    connectedUserId,
    connection,
    onClose,
}: ConnectedUserConnectionCredentialsDialogProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const useUpdateConnectedUserConnectionCredentialsMutation = useMemo(
        () => getUpdateConnectedUserConnectionCredentials(connectedUserId),
        [connectedUserId]
    );

    if (!componentDefinitions) {
        return null;
    }

    return (
        <ConnectionDialog
            componentDefinitions={componentDefinitions}
            connection={connection}
            connectionTagsQueryKey={ConnectionKeys.connectionTags}
            connectionsQueryKey={ConnectionKeys.allConnectedUserConnections(connectedUserId)}
            onClose={onClose}
            startInCredentialsMode
            useGetConnectionTagsQuery={useGetConnectionTagsQuery}
            useUpdateConnectionCredentialsMutation={useUpdateConnectedUserConnectionCredentialsMutation}
            useUpdateConnectionMutation={useUpdateConnectionMutation}
        />
    );
};

export default ConnectedUserConnectionCredentialsDialog;
