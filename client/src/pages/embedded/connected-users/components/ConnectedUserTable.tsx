import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import ConnectedUserDeleteDialog from '@/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import useConnectedUserSheetStore from '@/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {ConnectedUserModel, CredentialStatusModel} from '@/shared/middleware/embedded/connected-user';
import {useEnableConnectedUserMutation} from '@/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/shared/queries/embedded/connectedUsers.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const columnHelper = createColumnHelper<ConnectedUserModel>();

interface ConnectedUserTableProps {
    connectedUsers: ConnectedUserModel[];
}

const ConnectedUserTable = ({connectedUsers}: ConnectedUserTableProps) => {
    const [currentConnectedUserId, setCurrentConnectedUserId] = useState(0);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {setConnectedUserId, setConnectedUserSheetOpen} = useConnectedUserSheetStore();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const columns = useMemo(
        () => [
            columnHelper.accessor((row) => row.integrationInstances, {
                cell: (info) => {
                    let enabled = true;

                    for (const integrationInstance of info.getValue()!) {
                        if (integrationInstance.credentialStatus === CredentialStatusModel.Invalid) {
                            enabled = false;

                            break;
                        }
                    }

                    return (
                        <svg
                            aria-hidden="true"
                            className={twMerge('h-3 w-3', twMerge(enabled ? 'fill-success' : 'fill-destructive'))}
                            viewBox="0 0 6 6"
                        >
                            <circle cx={3} cy={3} r={3} />
                        </svg>
                    );
                },
                header: 'Status',
                id: 'status',
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
        [componentDefinitions, connectedUsers]
    );

    const reactTable = useReactTable<ConnectedUserModel>({
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
        <div className="w-full px-4 2xl:mx-auto 2xl:w-4/5">
            <Table className="table-auto">
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow key={headerGroup.id}>
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

                <TableBody className="divide-y divide-gray-200 bg-white">
                    {rows.map((row) => (
                        <TableRow className="cursor-pointer" key={row.id}>
                            {row.getVisibleCells().map((cell) => (
                                <TableCell
                                    className={twMerge(
                                        'whitespace-nowrap',
                                        !connectedUsers[row.index].enabled && 'text-muted-foreground',
                                        cell.id.endsWith('actions') && 'flex justify-end',
                                        cell.id.endsWith('enabled') && 'flex justify-center w-20',
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
                                >
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </TableCell>
                            ))}
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
