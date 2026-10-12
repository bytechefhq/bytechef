import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    RegisteredClientsQuery,
    useDeleteRegisteredClientMutation,
    useRegisteredClientsQuery,
    useRevokeRegisteredClientGrantsMutation,
} from '@/shared/middleware/graphql';
import {coreTableFeatures} from '@/shared/util/table-features';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, useTable} from '@tanstack/react-table';
import {ChevronDownIcon, ChevronRightIcon, KeyIcon, Trash2Icon, UserXIcon} from 'lucide-react';
import {Fragment, useCallback, useMemo, useState} from 'react';

import RegisteredClientGrantsTable from './components/RegisteredClientGrantsTable';
import formatDate from './utils/formatDate';

type RegisteredClientType = NonNullable<NonNullable<RegisteredClientsQuery['registeredClients']>[number]>;

const columnHelper = createColumnHelper<typeof coreTableFeatures, RegisteredClientType>();

const RegisteredClients = () => {
    const [clientToDelete, setClientToDelete] = useState<RegisteredClientType | undefined>(undefined);
    const [clientToRevoke, setClientToRevoke] = useState<RegisteredClientType | undefined>(undefined);
    const [expandedClientIds, setExpandedClientIds] = useState<string[]>([]);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useRegisteredClientsQuery();

    const deleteRegisteredClientMutation = useDeleteRegisteredClientMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['registeredClients']});

            setClientToDelete(undefined);
        },
    });

    const revokeRegisteredClientGrantsMutation = useRevokeRegisteredClientGrantsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['registeredClientGrants']});
            void queryClient.invalidateQueries({queryKey: ['registeredClients']});

            setClientToRevoke(undefined);
        },
    });

    const registeredClients = useMemo(
        () =>
            (data?.registeredClients ?? []).filter(
                (registeredClient): registeredClient is RegisteredClientType => registeredClient != null
            ),
        [data]
    );

    const toggleExpandedClient = useCallback(
        (clientId: string) =>
            setExpandedClientIds((current) =>
                current.includes(clientId) ? current.filter((id) => id !== clientId) : [...current, clientId]
            ),
        []
    );

    const columns = useMemo(
        () =>
            columnHelper.columns([
                columnHelper.display({
                    cell: (info) => {
                        const clientId = info.row.original.id as string;
                        const expanded = expandedClientIds.includes(clientId);

                        return (
                            <Button
                                aria-label="Show grants"
                                icon={
                                    expanded ? (
                                        <ChevronDownIcon className="h-4" />
                                    ) : (
                                        <ChevronRightIcon className="h-4" />
                                    )
                                }
                                onClick={() => toggleExpandedClient(clientId)}
                                size="icon"
                                variant="ghost"
                            />
                        );
                    },
                    header: '',
                    id: 'expand',
                }),
                columnHelper.accessor('clientName', {
                    cell: (info) => info.getValue() ?? '',
                    header: 'Name',
                }),
                columnHelper.accessor('clientId', {
                    cell: (info) => info.getValue() ?? '',
                    header: 'Client ID',
                }),
                columnHelper.accessor('clientIdIssuedAt', {
                    cell: (info) => formatDate(info.getValue()),
                    header: 'Issued',
                }),
                columnHelper.accessor('scopes', {
                    cell: (info) => (info.getValue() ?? []).join(', '),
                    header: 'Scopes',
                }),
                columnHelper.accessor('grantCount', {
                    cell: (info) => <Badge label={`${info.getValue() ?? 0} user(s)`} styleType="secondary-outline" />,
                    header: 'Grants',
                }),
                columnHelper.display({
                    cell: (info) =>
                        info.row.original.deletable ? (
                            <Button
                                aria-label="Delete client"
                                icon={<Trash2Icon className="h-4 text-destructive" />}
                                onClick={() => setClientToDelete(info.row.original)}
                                size="icon"
                                variant="ghost"
                            />
                        ) : (
                            <Button
                                aria-label="Revoke all grants"
                                icon={<UserXIcon className="h-4 text-destructive" />}
                                onClick={() => setClientToRevoke(info.row.original)}
                                size="icon"
                                variant="ghost"
                            />
                        ),
                    header: '',
                    id: 'actions',
                }),
            ]),
        [expandedClientIds, toggleExpandedClient]
    );

    const reactTable = useTable({
        columns,
        data: registeredClients,
        features: coreTableFeatures,
    });

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="OAuth2 clients registered with the embedded authorization server, typically created through Dynamic Client Registration."
                        position="main"
                        title="OAuth2 Clients"
                    />
                }
                leftSidebarOpen={false}
            >
                {registeredClients.length > 0 ? (
                    <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
                        <Table>
                            <TableHeader>
                                {reactTable.getHeaderGroups().map((headerGroup) => (
                                    <TableRow className="border-b-border/50" key={headerGroup.id}>
                                        {headerGroup.headers.map((header) => (
                                            <TableHead
                                                className="sticky top-0 z-10 bg-surface-neutral-primary p-3 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase"
                                                key={header.id}
                                            >
                                                {!header.isPlaceholder &&
                                                    flexRender(header.column.columnDef.header, header.getContext())}
                                            </TableHead>
                                        ))}
                                    </TableRow>
                                ))}
                            </TableHeader>

                            <TableBody>
                                {reactTable.getRowModel().rows.map((row) => (
                                    <Fragment key={row.id}>
                                        <TableRow className="border-b-border/50">
                                            {row.getAllCells().map((cell) => (
                                                <TableCell
                                                    className={
                                                        cell.id.endsWith('actions')
                                                            ? 'flex justify-end p-3'
                                                            : 'p-3 whitespace-nowrap'
                                                    }
                                                    key={cell.id}
                                                >
                                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                                </TableCell>
                                            ))}
                                        </TableRow>

                                        {expandedClientIds.includes(row.original.id as string) && (
                                            <TableRow className="border-b-border/50">
                                                <TableCell />

                                                <TableCell className="p-0" colSpan={row.getAllCells().length - 1}>
                                                    <RegisteredClientGrantsTable
                                                        registeredClientId={row.original.id as string}
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </Fragment>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                ) : (
                    <EmptyList
                        icon={<KeyIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Clients registered through Dynamic Client Registration will appear here."
                        title="No OAuth2 Clients"
                    />
                )}

                {clientToDelete && (
                    <AlertDialog
                        description="This action cannot be undone. This will permanently delete the OAuth2 client and revoke its authorizations."
                        isPending={deleteRegisteredClientMutation.isPending}
                        onCancel={() => setClientToDelete(undefined)}
                        onConfirm={() => deleteRegisteredClientMutation.mutate({id: clientToDelete.id as string})}
                        open={true}
                    />
                )}

                {clientToRevoke && (
                    <AlertDialog
                        confirmLabel="Revoke"
                        description="This signs every user out of every MCP server connected through this client. They must approve access again to reconnect."
                        isPending={revokeRegisteredClientGrantsMutation.isPending}
                        onCancel={() => setClientToRevoke(undefined)}
                        onConfirm={() =>
                            revokeRegisteredClientGrantsMutation.mutate({
                                registeredClientId: clientToRevoke.id as string,
                            })
                        }
                        open={true}
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default RegisteredClients;
