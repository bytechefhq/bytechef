import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import EmbeddedDataTableList from '@/ee/pages/embedded/data-tables/components/EmbeddedDataTableList';
import OwnerSelect from '@/ee/pages/embedded/shared/components/OwnerSelect';
import useEmbeddedConnectedUsers from '@/ee/pages/embedded/shared/components/useEmbeddedConnectedUsers';
import useDataTables from '@/shared/components/data-tables/components/hooks/useDataTables';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAssignEmbeddedDataTableOwnerMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Table2Icon} from 'lucide-react';
import {useState} from 'react';

const EmbeddedDataTables = () => {
    const [ownerId, setOwnerId] = useState<number | undefined>(undefined);

    const {connectedUsers} = useEmbeddedConnectedUsers();

    const queryClient = useQueryClient();

    const {error, isLoading, tables} = useDataTables({ownerId, type: 'EMBEDDED'});

    const assignOwnerMutation = useAssignEmbeddedDataTableOwnerMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['EmbeddedDataTables']});
        },
    });

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
                            </div>
                        }
                        title="Data Tables"
                    />
                }
            >
                {tables.length > 0 ? (
                    <EmbeddedDataTableList
                        connectedUsers={connectedUsers}
                        dataTables={tables}
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
