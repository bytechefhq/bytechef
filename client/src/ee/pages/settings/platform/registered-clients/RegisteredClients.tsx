import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    RegisteredClientsQuery,
    useDeleteRegisteredClientMutation,
    useRegisteredClientsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {KeyIcon, Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';

type RegisteredClientType = NonNullable<NonNullable<RegisteredClientsQuery['registeredClients']>[number]>;

const columnHelper = createColumnHelper<RegisteredClientType>();

const formatDate = (value: unknown): string => {
    if (value == null) {
        return '';
    }

    const date = new Date(value as number);

    return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
};

const RegisteredClients = () => {
    const [clientToDelete, setClientToDelete] = useState<RegisteredClientType | undefined>(undefined);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useRegisteredClientsQuery();

    const deleteRegisteredClientMutation = useDeleteRegisteredClientMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['registeredClients']});

            setClientToDelete(undefined);
        },
    });

    const registeredClients = useMemo(
        () =>
            (data?.registeredClients ?? []).filter(
                (registeredClient): registeredClient is RegisteredClientType => registeredClient != null
            ),
        [data]
    );

    const columns = useMemo(
        () => [
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
            columnHelper.display({
                cell: (info) => (
                    <Button
                        aria-label="Delete client"
                        icon={<Trash2Icon className="h-4 text-destructive" />}
                        onClick={() => setClientToDelete(info.row.original)}
                        size="icon"
                        variant="ghost"
                    />
                ),
                header: '',
                id: 'actions',
            }),
        ],
        []
    );

    const reactTable = useReactTable<RegisteredClientType>({
        columns,
        data: registeredClients,
        getCoreRowModel: getCoreRowModel(),
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
                                    <TableRow className="border-b-border/50" key={row.id}>
                                        {row.getVisibleCells().map((cell) => (
                                            <TableCell
                                                className={
                                                    cell.id.endsWith('actions')
                                                        ? 'flex justify-end'
                                                        : 'whitespace-nowrap'
                                                }
                                                key={cell.id}
                                            >
                                                {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                            </TableCell>
                                        ))}
                                    </TableRow>
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
                    <AlertDialog onOpenChange={() => setClientToDelete(undefined)} open={true}>
                        <AlertDialogContent>
                            <AlertDialogHeader>
                                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                                <AlertDialogDescription>
                                    This action cannot be undone. This will permanently delete the OAuth2 client and
                                    revoke its authorizations.
                                </AlertDialogDescription>
                            </AlertDialogHeader>

                            <AlertDialogFooter>
                                <AlertDialogCancel>Cancel</AlertDialogCancel>

                                <AlertDialogAction
                                    className="bg-destructive"
                                    onClick={() =>
                                        deleteRegisteredClientMutation.mutate({id: clientToDelete.id as string})
                                    }
                                >
                                    Delete
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default RegisteredClients;
