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
import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import ApiClientDialog from '@/ee/pages/automation/api-platform/api-clients/components/ApiClientDialog';
import {ApiClient} from '@/ee/shared/middleware/automation/api-platform';
import {useDeleteApiClientMutation} from '@/shared/mutations/platform/apiClients.mutations';
import {ApiClientKeys} from '@/shared/queries/platform/apiClients.queries';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {EditIcon, Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const columnHelper = createColumnHelper<ApiClient>();

interface ApiClientTableProps {
    apiClients: ApiClient[];
}

const ApiClientDeleteDialog = ({apiClientId, onClose}: {apiClientId: number; onClose: () => void}) => {
    const queryClient = useQueryClient();

    const deleteApiClientMutation = useDeleteApiClientMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiClientKeys.apiClients,
            });

            onClose();
        },
    });

    const handleClick = () => {
        deleteApiClientMutation.mutate(apiClientId);
    };

    return (
        <AlertDialog onOpenChange={onClose} open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete API key.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-destructive" onClick={handleClick}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

const ApiClientTable = ({apiClients}: ApiClientTableProps) => {
    const [currentApiClient, setCurrentApiClient] = useState<ApiClient>();
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const columns = useMemo(
        () => [
            columnHelper.accessor('name', {
                cell: (info) => info.getValue() ?? '',
                header: 'Name',
            }),
            columnHelper.accessor('secretKey', {
                cell: (info) => info.getValue() ?? '',
                header: 'Secret Key',
            }),
            columnHelper.accessor('createdDate', {
                cell: (info) => `${info.getValue()?.toLocaleDateString()} ${info.getValue()?.toLocaleTimeString()}`,
                header: 'Created Date',
            }),
            columnHelper.accessor('lastUsedDate', {
                cell: (info) =>
                    `${info.getValue()?.toLocaleDateString() ?? ''} ${info.getValue()?.toLocaleTimeString() ?? ''}`,
                header: 'Last Used Date',
            }),
            columnHelper.display({
                cell: (info) => (
                    <>
                        <Button
                            onClick={() => {
                                setCurrentApiClient(info.row.original);
                                setShowEditDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        >
                            <EditIcon className="size-4" />
                        </Button>

                        <Button
                            onClick={() => {
                                setCurrentApiClient(info.row.original);
                                setShowDeleteDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        >
                            <Trash2Icon className="h-4 text-destructive" />
                        </Button>
                    </>
                ),
                header: '',
                id: 'actions',
            }),
        ],
        []
    );

    const reactTable = useReactTable<ApiClient>({
        columns,
        data: apiClients,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
            <p>
                API clients and their secret API keys are listed below. Please note that we do not display your secret
                API keys again after you generate them.
            </p>

            <p>
                Do not expose it in the browser or other client-side code. In order to protect the security of your
                account, ByteChef may also automatically disable any API key that we have found has leaked publicly.
            </p>

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
                            {row.getVisibleCells().map((cell) => (
                                <TableCell
                                    className={twMerge(
                                        'whitespace-nowrap',
                                        cell.id.endsWith('actions') && 'flex justify-end',
                                        cell.id.endsWith('enabled') && 'ml-6 flex'
                                    )}
                                    key={cell.id}
                                    onClick={(event) => {
                                        if (cell.id.endsWith('actions')) {
                                            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                            const target = event.target as any;

                                            if (target.getAttribute('data-action') === 'delete') {
                                                setCurrentApiClient(apiClients[target.getAttribute('data-index')]);

                                                setShowDeleteDialog(true);
                                            } else if (target.getAttribute('data-action') === 'edit') {
                                                setCurrentApiClient(apiClients[target.getAttribute('data-index')]);

                                                setShowEditDialog(true);
                                            }
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

            {showDeleteDialog && currentApiClient && (
                <ApiClientDeleteDialog apiClientId={currentApiClient.id!} onClose={() => setShowDeleteDialog(false)} />
            )}

            {showEditDialog && (
                <ApiClientDialog apiClient={currentApiClient} onClose={() => setShowEditDialog(false)} />
            )}
        </div>
    );
};

export default ApiClientTable;
