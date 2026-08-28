import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import EmbeddedDataTableList from '@/ee/pages/embedded/data-tables/components/EmbeddedDataTableList';
import OwnerSelect from '@/ee/pages/embedded/shared/components/OwnerSelect';
import useEmbeddedConnectedUsers from '@/ee/pages/embedded/shared/components/useEmbeddedConnectedUsers';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAssignEmbeddedDataTableOwnerMutation, useEmbeddedDataTablesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Table2Icon} from 'lucide-react';
import {useState} from 'react';

const EmbeddedDataTables = () => {
    const [ownerId, setOwnerId] = useState<number | undefined>(undefined);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {connectedUsers} = useEmbeddedConnectedUsers();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useEmbeddedDataTablesQuery({
        environmentId: String(currentEnvironmentId),
        ownerId: ownerId === undefined ? undefined : String(ownerId),
    });

    const assignOwnerMutation = useAssignEmbeddedDataTableOwnerMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['EmbeddedDataTables']});
        },
    });

    const dataTables = data?.embeddedDataTables ?? [];

    const handleAssign = (dataTableId: string, newOwnerId: number | undefined) => {
        assignOwnerMutation.mutate({
            input: {
                dataTableId,
                ownerId: newOwnerId === undefined ? undefined : String(newOwnerId),
            },
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <div className="flex items-center gap-1">
                                <OwnerSelect
                                    connectedUsers={connectedUsers}
                                    noOwnerLabel="All owners"
                                    onChange={setOwnerId}
                                    ownerId={ownerId}
                                />

                                <EnvironmentSelect />
                            </div>
                        }
                        title="Data Tables"
                    />
                }
            >
                {dataTables.length > 0 ? (
                    <EmbeddedDataTableList
                        connectedUsers={connectedUsers}
                        dataTables={dataTables}
                        onAssign={handleAssign}
                    />
                ) : (
                    <EmptyList
                        icon={<Table2Icon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Data tables you create appear here, where you can assign them to an account."
                        title="No Data Tables"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default EmbeddedDataTables;
