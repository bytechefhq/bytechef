import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import OrganizationConnectionDeleteDialog from '@/pages/settings/platform/organization-connections/components/OrganizationConnectionDeleteDialog';
import OrganizationConnectionsTable from '@/pages/settings/platform/organization-connections/components/OrganizationConnectionsTable';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AuthorizationType,
    OrganizationConnection,
    useCreateOrganizationConnectionMutation,
    useDeleteOrganizationConnectionMutation,
    useOrganizationConnectionsQuery,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {UseMutationResult} from '@tanstack/react-query';
import {Link2Icon, PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {Navigate} from 'react-router-dom';

const OrganizationConnections = () => {
    // All hooks must run unconditionally (rules of hooks). The EE gate is applied right before the
    // render-tree decision below; the gate is defense-in-depth on top of the server-side guard in
    // OrganizationConnectionFacadeImpl, so if CE users somehow reach this route the server
    // rejects anything they could submit.
    const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
    const [connectionToDelete, setConnectionToDelete] = useState<OrganizationConnection | undefined>(undefined);

    const edition = useApplicationInfoStore((state) => state.application?.edition);

    const {
        data,
        error: connectionsError,
        isLoading: isConnectionsLoading,
        refetch,
    } = useOrganizationConnectionsQuery({});

    const connections = data?.organizationConnections ?? [];

    /**
     * Adapts the organization create mutation to the shape ConnectionDialog injects. The dialog hands over a flat
     * connection and expects a numeric id back; createOrganizationConnection takes an input wrapper and returns the id
     * as a string.
     */
    const useCreateOrganizationConnectionAdapter = (mutationProps: {
        onError?: (error: Error, variables: ConnectionI) => void;
        onSuccess?: (result: number, variables: ConnectionI) => void;
    }) => {
        const mutation = useCreateOrganizationConnectionMutation({
            onError: (error: Error, variables) =>
                mutationProps.onError?.(error, variables.input as unknown as ConnectionI),
            onSuccess: (result, variables) => {
                setIsCreateDialogOpen(false);

                refetch();

                mutationProps.onSuccess?.(
                    Number(result.createOrganizationConnection),
                    variables.input as unknown as ConnectionI
                );
            },
        });

        return {
            ...mutation,
            mutate: (connection: ConnectionI) =>
                mutation.mutate({
                    input: {
                        authorizationType: (connection.authorizationType || undefined) as AuthorizationType | undefined,
                        componentName: connection.componentName ?? '',
                        connectionVersion: connection.connectionVersion ?? 1,
                        environmentId: Number(connection.environmentId ?? 0),
                        name: connection.name ?? '',
                        parameters: (connection.parameters ?? {}) as Record<string, unknown>,
                        tags: (connection.tags ?? []).map((tag) => ({
                            id: tag.id == null ? undefined : String(tag.id),
                            name: tag.name,
                        })),
                    },
                }),
        } as unknown as UseMutationResult<number, Error, ConnectionI, unknown>;
    };

    const deleteMutation = useDeleteOrganizationConnectionMutation({
        onError: () => {
            // Close the dialog on failure so the global useFetchInterceptor toast is not hidden
            // behind the dialog; the user can retry from the table. Leaving the dialog open after
            // a failed delete would stack toasts on each retry and mask the mutation state.
            setConnectionToDelete(undefined);
        },
        onSuccess: () => {
            setConnectionToDelete(undefined);

            refetch();
        },
    });

    const componentDefinitionsQueryResult = useGetComponentDefinitionsQuery({connectionDefinitions: true});
    const componentDefinitions = componentDefinitionsQueryResult.data;

    const connectionTagsQueryResult = useGetConnectionTagsQuery();

    const handleDeleteConfirm = (connectionId: string) => {
        deleteMutation.mutate({connectionId});
    };

    if (edition !== undefined && edition !== EditionType.EE) {
        return <Navigate replace to="/settings" />;
    }

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Manage organization-wide shared connections."
                    position="main"
                    right={
                        connections.length > 0 ? (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <Button
                                    icon={<PlusIcon />}
                                    label="New Connection"
                                    onClick={() => setIsCreateDialogOpen(true)}
                                />
                            </div>
                        ) : (
                            <EnvironmentSelect />
                        )
                    }
                    title="Organization Connections"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[connectionsError]} loading={isConnectionsLoading}>
                {connections.length > 0 ? (
                    <OrganizationConnectionsTable
                        connections={connections}
                        onDeleteClick={(connection) => setConnectionToDelete(connection)}
                    />
                ) : (
                    <EmptyList
                        button={
                            <Button
                                icon={<PlusIcon />}
                                label="New Connection"
                                onClick={() => setIsCreateDialogOpen(true)}
                            />
                        }
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="No organization connections have been created yet."
                        title="No Organization Connections"
                    />
                )}
            </PageLoader>

            {isCreateDialogOpen && (
                <ConnectionDialog
                    componentDefinitions={componentDefinitions ?? []}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    fixedVisibility="ORGANIZATION"
                    onClose={() => setIsCreateDialogOpen(false)}
                    useCreateConnectionMutation={useCreateOrganizationConnectionAdapter}
                    useGetConnectionTagsQuery={() => connectionTagsQueryResult}
                />
            )}

            {connectionToDelete && (
                <OrganizationConnectionDeleteDialog
                    connection={connectionToDelete}
                    onClose={() => setConnectionToDelete(undefined)}
                    onConfirm={handleDeleteConfirm}
                />
            )}
        </LayoutContainer>
    );
};

export default OrganizationConnections;
