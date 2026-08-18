import {AutomationHubKeys} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {ConnectionApi} from '@/ee/shared/middleware/embedded/public';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ReactNode} from 'react';

const HUB_CONNECTION_TAGS_QUERY_KEY = ['automationHub', 'tags'] as const;

interface CreateConnectionMutationPropsI {
    onError?: (error: Error, variables: ConnectionI) => void;
    onSuccess?: (result: number, variables: ConnectionI) => void;
}

interface ReauthorizeConnectionMutationPropsI {
    onError?: (error: Error, variables: ConnectionI) => void;
    onSuccess?: (result: void, variables: ConnectionI) => void;
}

const useCreateHubConnectionMutation = (mutationProps?: CreateConnectionMutationPropsI) =>
    useMutation<number, Error, ConnectionI>({
        mutationFn: (connection) =>
            new ConnectionApi().createFrontendConnection({
                componentName: connection.componentName,
                createConnectionRequest: {
                    authorizationType: connection.authorizationType,
                    connectionVersion: connection.connectionVersion,
                    name: connection.name,
                    parameters: connection.parameters,
                },
            }),
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

const useHubConnectionTagsQuery = () => useQuery({queryFn: async () => [], queryKey: HUB_CONNECTION_TAGS_QUERY_KEY});

/**
 * `existingConnectionId` isn't part of `ConnectionI`, so it can't be read off the mutation's
 * variables the way `useCreateHubConnectionMutation` reads `componentName`. `ConnectionDialog`'s own
 * create-path payload (`getNewConnection()`) never carries an `id` either — see `HubConnectionDialog`
 * below for why `connection` is deliberately prefilled without one. So the id is closed over here
 * instead, the same "hook factory" shape `getCreateConnectedUserConnection` uses in
 * `connections.mutations.ts` for the same reason (closing a connected-user id over a mutation hook).
 */
const getReauthorizeHubConnectionMutation =
    (existingConnectionId: number) => (mutationProps?: ReauthorizeConnectionMutationPropsI) =>
        useMutation<void, Error, ConnectionI>({
            mutationFn: (connection) =>
                new ConnectionApi().reauthorizeFrontendConnection({
                    id: existingConnectionId,
                    reauthorizeConnectionRequest: {parameters: connection.parameters},
                }),
            onError: mutationProps?.onError,
            onSuccess: mutationProps?.onSuccess,
        });

interface HubConnectionDialogProps {
    componentName: string;
    existingConnectionId?: number;
    existingConnectionVersion?: number;
    onClose: () => void;
    onCreated?: (id: number) => void;
    triggerNode?: ReactNode;
}

/**
 * Wraps the shared {@link ConnectionDialog} with the hub's own queries and mutations, so it can be
 * used both to create a brand-new connection and — when `existingConnectionId` is set — to
 * reconnect (reauthorize) an existing one for the same component. Reconnect deliberately prefills
 * `connection` WITHOUT an `id`: `ConnectionDialog` only shows the property/authorization fields (the
 * ones a reauthorize actually needs) when `connection?.id` is falsy — with an `id` present it
 * switches into its "rename only" edit mode instead, which has no way to resubmit credentials.
 *
 * `existingConnectionVersion` is the reconnected connection's own `Connection.connectionVersion`,
 * so the definition lookup (and the prefilled `connection`) render the authorization fields for
 * the version the connection is actually AT, rather than always version 1 — a component whose
 * connection moved to version 2+ would otherwise show the wrong fields on reconnect. It falls back
 * to 1 only when absent, which also keeps the create path (no `existingConnectionId`, and so no
 * version to carry) unchanged.
 */
const HubConnectionDialog = ({
    componentName,
    existingConnectionId,
    existingConnectionVersion,
    onClose,
    onCreated,
    triggerNode,
}: HubConnectionDialogProps) => {
    const connectionVersion = existingConnectionVersion ?? 1;

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName,
        componentVersion: connectionVersion,
    });
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentTitle = componentDefinition?.title || componentName;

    return (
        <ConnectionDialog
            componentDefinition={componentDefinition}
            componentDefinitions={componentDefinitions || []}
            connection={
                existingConnectionId
                    ? {
                          componentName,
                          connectionVersion,
                          name: componentTitle,
                          parameters: {},
                      }
                    : undefined
            }
            connectionTagsQueryKey={HUB_CONNECTION_TAGS_QUERY_KEY}
            connectionsQueryKey={AutomationHubKeys.connections}
            description={existingConnectionId ? 'Re-enter your credentials to reconnect this account.' : undefined}
            onClose={onClose}
            onConnectionCreate={onCreated}
            title={existingConnectionId ? `Reconnect ${componentTitle}` : undefined}
            triggerNode={triggerNode}
            useCreateConnectionMutation={useCreateHubConnectionMutation}
            useGetConnectionTagsQuery={useHubConnectionTagsQuery}
            useUpdateConnectionMutation={
                existingConnectionId ? getReauthorizeHubConnectionMutation(existingConnectionId) : undefined
            }
        />
    );
};

export default HubConnectionDialog;
