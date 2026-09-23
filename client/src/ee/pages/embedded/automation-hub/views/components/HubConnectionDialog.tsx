import {AutomationHubKeys} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {ConnectionApi, CredentialStatus} from '@/ee/shared/middleware/embedded/public';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMutation, useQuery} from '@tanstack/react-query';

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

const useHubConnectionTagsQuery = () =>
    useQuery({queryFn: () => Promise.resolve([]), queryKey: HUB_CONNECTION_TAGS_QUERY_KEY});

const useReauthorizeHubConnectionMutation = (mutationProps?: ReauthorizeConnectionMutationPropsI) =>
    useMutation<void, Error, ConnectionI>({
        mutationFn: (connection) =>
            new ConnectionApi().reauthorizeFrontendConnection({
                id: connection.id!,
                reauthorizeConnectionRequest: {parameters: connection.parameters},
            }),
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface HubConnectionDialogProps {
    componentName: string;
    existingConnectionCredentialStatus?: CredentialStatus;
    existingConnectionId?: number;
    existingConnectionVersion?: number;
    onClose: () => void;
    onCreated?: (id: number) => void;
}

const HubConnectionDialog = ({
    componentName,
    existingConnectionCredentialStatus,
    existingConnectionId,
    existingConnectionVersion,
    onClose,
    onCreated,
}: HubConnectionDialogProps) => {
    const connectionVersion = existingConnectionVersion ?? 1;

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName,
        componentVersion: connectionVersion,
    });
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    if (!componentDefinition) {
        return null;
    }

    const componentTitle = componentDefinition.title || componentName;

    return (
        <ConnectionDialog
            componentDefinition={componentDefinition}
            componentDefinitions={componentDefinitions || []}
            connection={
                existingConnectionId
                    ? {
                          componentName,
                          connectionVersion,
                          credentialStatus: existingConnectionCredentialStatus,
                          id: existingConnectionId,
                          name: componentTitle,
                          parameters: {},
                      }
                    : undefined
            }
            connectionTagsQueryKey={HUB_CONNECTION_TAGS_QUERY_KEY}
            connectionsQueryKey={AutomationHubKeys.connections}
            onClose={onClose}
            onConnectionCreate={onCreated}
            startInCredentialsMode={!!existingConnectionId}
            title={existingConnectionId ? `Reconnect ${componentTitle}` : undefined}
            useCreateConnectionMutation={useCreateHubConnectionMutation}
            useGetConnectionTagsQuery={useHubConnectionTagsQuery}
            useUpdateConnectionCredentialsMutation={
                existingConnectionId ? useReauthorizeHubConnectionMutation : undefined
            }
        />
    );
};

export default HubConnectionDialog;
