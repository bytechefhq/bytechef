import {getCreateConnectedUserConnection} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import * as Portal from '@radix-ui/react-portal';
import {useMemo} from 'react';

interface ConnectedUserNewConnectionDialogProps {
    componentName: string;
    connectedUserId: number;
    onClose: () => void;
    onConnectionCreate: (connectionId: number) => void;
}

const ConnectedUserNewConnectionDialog = ({
    componentName,
    connectedUserId,
    onClose,
    onConnectionCreate,
}: ConnectedUserNewConnectionDialogProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentVersion = componentDefinitions?.find(
        (componentDefinition) => componentDefinition.name === componentName
    )?.version;

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName, componentVersion: componentVersion!},
        componentVersion != null
    );

    const useCreateConnectedUserConnectionMutation = useMemo(
        () => getCreateConnectedUserConnection(connectedUserId),
        [connectedUserId]
    );

    if (!componentDefinitions || !componentDefinition) {
        return null;
    }

    return (
        <Portal.Root>
            <ConnectionDialog
                componentDefinition={componentDefinition}
                componentDefinitions={componentDefinitions}
                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                connectionsQueryKey={ConnectionKeys.connections}
                onClose={onClose}
                onConnectionCreate={onConnectionCreate}
                useCreateConnectionMutation={useCreateConnectedUserConnectionMutation}
                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
            />
        </Portal.Root>
    );
};

export default ConnectedUserNewConnectionDialog;
