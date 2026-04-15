import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import OrganizationConnectionDeleteDialog from '@/pages/settings/platform/organization-connections/components/OrganizationConnectionDeleteDialog';
import OrganizationConnectionDialog from '@/pages/settings/platform/organization-connections/components/OrganizationConnectionDialog';
import OrganizationConnectionsTable from '@/pages/settings/platform/organization-connections/components/OrganizationConnectionsTable';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    OrganizationConnection,
    useCreateOrganizationConnectionMutation,
    useDeleteOrganizationConnectionMutation,
    useOrganizationConnectionsQuery,
} from '@/shared/middleware/graphql';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Link2Icon, PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {Navigate} from 'react-router-dom';

const OrganizationConnections = () => {
    // All hooks must run unconditionally (rules of hooks). The EE gate is applied right before the
    // render-tree decision below; the gate is defense-in-depth on top of the server-side guard in
    // OrganizationConnectionFacadeImpl, so if CE users somehow reach this route the server
    // rejects anything they could submit.
    const edition = useApplicationInfoStore((state) => state.application?.edition);

    const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
    const [connectionToDelete, setConnectionToDelete] = useState<OrganizationConnection | undefined>(undefined);

    const {
        data,
        error: connectionsError,
        isLoading: isConnectionsLoading,
        refetch,
    } = useOrganizationConnectionsQuery({});

    const connections = data?.organizationConnections ?? [];

    const createMutation = useCreateOrganizationConnectionMutation({
        onSuccess: () => {
            setIsCreateDialogOpen(false);

            refetch();
        },
    });

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

    const handleCreate = (formData: {
        componentName: string;
        connectionVersion: number;
        environmentId: number;
        name: string;
        parameters: Record<string, unknown>;
    }) => {
        createMutation.mutate({
            input: {
                componentName: formData.componentName,
                connectionVersion: formData.connectionVersion,
                environmentId: formData.environmentId,
                name: formData.name,
                parameters: formData.parameters,
            },
        });
    };

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
                <OrganizationConnectionDialog onClose={() => setIsCreateDialogOpen(false)} onSave={handleCreate} />
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
