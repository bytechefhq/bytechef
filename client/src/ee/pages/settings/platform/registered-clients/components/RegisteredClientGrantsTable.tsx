import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    RegisteredClientGrantsQuery,
    useRegisteredClientGrantsQuery,
    useRevokeRegisteredClientGrantMutation,
} from '@/shared/middleware/graphql';
import {coreTableFeatures} from '@/shared/util/table-features';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, useTable} from '@tanstack/react-table';
import {UserXIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import formatDate from '../utils/formatDate';

type RegisteredClientGrantType = NonNullable<
    NonNullable<RegisteredClientGrantsQuery['registeredClientGrants']>[number]
>;

interface RegisteredClientGrantsTableProps {
    registeredClientId: string;
}

const MISSING_DATE = '—';

const columnHelper = createColumnHelper<typeof coreTableFeatures, RegisteredClientGrantType>();

const RegisteredClientGrantsTable = ({registeredClientId}: RegisteredClientGrantsTableProps) => {
    const [grantToRevoke, setGrantToRevoke] = useState<RegisteredClientGrantType | undefined>(undefined);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useRegisteredClientGrantsQuery({registeredClientId});

    const revokeGrantMutation = useRevokeRegisteredClientGrantMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['registeredClientGrants']});
            void queryClient.invalidateQueries({queryKey: ['registeredClients']});

            setGrantToRevoke(undefined);
        },
    });

    const grants = useMemo(
        () => (data?.registeredClientGrants ?? []).filter((grant): grant is RegisteredClientGrantType => grant != null),
        [data]
    );

    const columns = useMemo(
        () =>
            columnHelper.columns([
                columnHelper.accessor('principalName', {
                    cell: (info) => info.getValue() ?? '',
                    header: 'User',
                }),
                columnHelper.accessor('mcpServers', {
                    cell: (info) => (info.getValue() ?? []).join(', '),
                    header: 'MCP servers',
                }),
                columnHelper.accessor('scopes', {
                    cell: (info) => (info.getValue() ?? []).join(', '),
                    header: 'Scopes',
                }),
                columnHelper.accessor('authorizedAt', {
                    cell: (info) => formatDate(info.getValue()) || MISSING_DATE,
                    header: 'Authorized',
                }),
                columnHelper.accessor('lastIssuedAt', {
                    cell: (info) => formatDate(info.getValue()) || MISSING_DATE,
                    header: 'Last used',
                }),
                columnHelper.accessor('expiresAt', {
                    cell: (info) => formatDate(info.getValue()) || MISSING_DATE,
                    header: 'Expires',
                }),
                columnHelper.display({
                    cell: (info) => (
                        <Button
                            aria-label={`Revoke ${info.row.original.principalName}`}
                            icon={<UserXIcon className="h-4 text-destructive" />}
                            onClick={() => setGrantToRevoke(info.row.original)}
                            size="icon"
                            variant="ghost"
                        />
                    ),
                    header: '',
                    id: 'actions',
                }),
            ]),
        []
    );

    const reactTable = useTable({
        columns,
        data: grants,
        features: coreTableFeatures,
    });

    if (isLoading) {
        return <div className="p-3 text-xs text-content-neutral-secondary">Loading grants...</div>;
    }

    if (error) {
        return (
            <div className="p-3 text-xs text-content-destructive-primary" role="alert">
                The users who authorized this client could not be loaded.
            </div>
        );
    }

    if (grants.length === 0) {
        return <div className="p-3 text-xs text-content-neutral-secondary">No user has authorized this client.</div>;
    }

    return (
        <>
            <Table>
                <TableHeader>
                    {reactTable.getHeaderGroups().map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <TableHead
                                    className="p-3 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase"
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
                            {row.getAllCells().map((cell) => (
                                <TableCell
                                    className={
                                        cell.id.endsWith('actions') ? 'flex justify-end p-3' : 'p-3 whitespace-nowrap'
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

            {grantToRevoke && (
                <AlertDialog
                    confirmLabel="Continue"
                    description="This signs the user out of every MCP server connected through this client. They must approve access again to reconnect."
                    isPending={revokeGrantMutation.isPending}
                    onCancel={() => setGrantToRevoke(undefined)}
                    onConfirm={() =>
                        revokeGrantMutation.mutate({
                            principalName: grantToRevoke.principalName as string,
                            registeredClientId,
                        })
                    }
                    open={true}
                />
            )}
        </>
    );
};

export default RegisteredClientGrantsTable;
