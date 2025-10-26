import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import ConnectedUserDeleteDialog from '@/ee/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import CredentialsStatus from '@/ee/pages/embedded/connected-users/components/CredentialsStatus';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {ConnectedUser, CredentialStatus} from '@/ee/shared/middleware/embedded/connected-user';
import {useEnableConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const columnHelper = createColumnHelper<ConnectedUser>();

interface ConnectedUserTableProps {
    connectedUsers: ConnectedUser[];
}

const ConnectedUserTable = ({connectedUsers}: ConnectedUserTableProps) => {
    const [currentConnectedUserId, setCurrentConnectedUserId] = useState(0);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {setConnectedUserId, setConnectedUserSheetOpen} = useConnectedUserSheetStore(
        useShallow((state) => ({
            setConnectedUserId: state.setConnectedUserId,
            setConnectedUserSheetOpen: state.setConnectedUserSheetOpen,
        }))
    );

    const {data: environmentsQuery} = useEnvironmentsQuery();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const columns = useMemo(
        () => [
            columnHelper.accessor((row) => row.integrationInstances, {
                cell: (info) => {
                    const integrationInstances = info.getValue() ?? [];

                    if (integrationInstances.length === 0) {
                        return <CredentialsStatus />;
                    }

                    let enabled = true;

                    for (const integrationInstance of integrationInstances) {
                        if (integrationInstance.credentialStatus === CredentialStatus.Invalid) {
                            enabled = false;

                            break;
                        }
                    }

                    return <CredentialsStatus enabled={enabled} />;
                },
                header: 'Status',
                id: 'status',
            }),
            columnHelper.accessor('externalId', {
                cell: (info) => info.getValue() ?? '',
                header: 'External Id',
            }),
            columnHelper.accessor('name', {
                cell: (info) => info.getValue() ?? '',
                header: 'Name',
            }),
            columnHelper.accessor('email', {
                cell: (info) => info.getValue() ?? '',
                header: 'Email',
            }),
            columnHelper.accessor('integrationInstances', {
                cell: (info) => {
                    return (
                        <div className="flex space-x-0.5">
                            {info.getValue() ? (
                                info.getValue()?.map((integrationInstance) => {
                                    const componentDefinition = componentDefinitions?.find(
                                        (componentDefinition) =>
                                            componentDefinition.name === integrationInstance.componentName
                                    );

                                    return (
                                        componentDefinition && (
                                            <InlineSVG
                                                className="mr-2 size-6 flex-none"
                                                key={componentDefinition.name!}
                                                src={componentDefinition.icon!}
                                            />
                                        )
                                    );
                                })
                            ) : (
                                <></>
                            )}
                        </div>
                    );
                },
                header: 'Integrations',
            }),
            columnHelper.accessor('environmentId', {
                cell: (info) =>
                    environmentsQuery?.environments?.find((environment) => +environment!.id! === info.getValue())?.name,
                header: 'Environment',
            }),
            columnHelper.accessor('createdDate', {
                cell: (info) => `${info?.getValue()?.toLocaleDateString()} ${info?.getValue()?.toLocaleTimeString()}`,
                header: 'Created Date',
            }),
            columnHelper.display({
                cell: (info) => (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem data-action="open-sheet" data-index={info.row.index.toString()}>
                                Details
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                data-action={connectedUsers[info.row.index].enabled ? 'disable' : 'enable'}
                                data-index={info.row.index.toString()}
                            >
                                {connectedUsers[info.row.index].enabled ? 'Disable' : 'Enable'}
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                className="text-destructive"
                                data-action="delete"
                                data-index={info.row.index.toString()}
                            >
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                ),
                header: '',
                id: 'actions',
            }),
        ],
        [componentDefinitions, connectedUsers, environmentsQuery?.environments]
    );

    const reactTable = useReactTable<ConnectedUser>({
        columns,
        data: connectedUsers,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (connectedUsers[index].id) {
            setConnectedUserId(connectedUsers[index].id!);

            setConnectedUserSheetOpen(true);
        }
    };

    const queryClient = useQueryClient();

    const enableConnectedUserMutation = useEnableConnectedUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            <Table className="table-auto">
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <TableHead
                                    className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
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
                    {rows.map((row) => (
                        <TableRow className="cursor-pointer border-b-border/50" key={row.id}>
                            {row.getVisibleCells().map((cell) => {
                                let width = '';
                                if (cell.id.endsWith('integrationInstances')) {
                                    width = '30%';
                                } else if (cell.id.endsWith('name')) {
                                    width = '15%';
                                } else if (cell.id.endsWith('email')) {
                                    width = '15%';
                                } else if (cell.id.endsWith('externalId')) {
                                    width = '15%';
                                }

                                return (
                                    <TableCell
                                        className={twMerge(
                                            'whitespace-nowrap',
                                            !connectedUsers[row.index].enabled && 'text-muted-foreground',
                                            cell.id.endsWith('actions') && 'flex justify-end',
                                            cell.id.endsWith('enabled') && 'flex justify-center',
                                            cell.id.endsWith('status') && 'pl-8'
                                        )}
                                        key={cell.id}
                                        onClick={(event) => {
                                            if (cell.id.endsWith('actions')) {
                                                /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                                const target = event.target as any;

                                                if (target.getAttribute('data-action') === 'delete') {
                                                    setCurrentConnectedUserId(
                                                        connectedUsers[target.getAttribute('data-index')].id!
                                                    );

                                                    setShowDeleteDialog(true);
                                                } else if (target.getAttribute('data-action') === 'disable') {
                                                    enableConnectedUserMutation.mutate({
                                                        enable: false,
                                                        id: connectedUsers[target.getAttribute('data-index')].id!,
                                                    });
                                                } else if (target.getAttribute('data-action') === 'enable') {
                                                    enableConnectedUserMutation.mutate({
                                                        enable: true,
                                                        id: connectedUsers[target.getAttribute('data-index')].id!,
                                                    });
                                                } else if (target.getAttribute('data-action') === 'open-sheet') {
                                                    setConnectedUserId(
                                                        connectedUsers[target.getAttribute('data-index')].id!
                                                    );
                                                    setConnectedUserSheetOpen(true);
                                                }
                                            } else {
                                                handleRowClick(row.index);
                                            }
                                        }}
                                        width={width}
                                    >
                                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>

            {showDeleteDialog && (
                <ConnectedUserDeleteDialog
                    connectedUserId={currentConnectedUserId}
                    onClose={() => setShowDeleteDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectedUserTable;
