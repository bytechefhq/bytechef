import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {ApiKey} from '@/shared/middleware/graphql';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {EditIcon, Trash2Icon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const columnHelper = createColumnHelper<ApiKey>();

interface ApiKeyTableProps {
    apiKeys: ApiKey[];
}

const ApiKeyTable = ({apiKeys}: ApiKeyTableProps) => {
    const {setCurrentApiKey, setShowDeleteDialog, setShowEditDialog} = useApiKeysStore(
        useShallow((state) => ({
            setCurrentApiKey: state.setCurrentApiKey,
            setShowDeleteDialog: state.setShowDeleteDialog,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

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
                cell: (info) =>
                    `${new Date(info.getValue()).toLocaleDateString()} ${new Date(info.getValue()).toLocaleTimeString()}`,
                header: 'Created Date',
            }),
            columnHelper.accessor('lastUsedDate', {
                cell: (info) =>
                    `${new Date(info.getValue()).toLocaleDateString() ?? ''} ${new Date(info.getValue()).toLocaleTimeString() ?? ''}`,
                header: 'Last Used Date',
            }),
            columnHelper.accessor('createdBy', {
                cell: (info) => `${info.getValue()}`,
                header: 'Created By',
            }),
            columnHelper.display({
                cell: (info) => (
                    <>
                        <Button
                            icon={<EditIcon className="size-4" />}
                            onClick={() => {
                                setCurrentApiKey(info.row.original);
                                setShowEditDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        />

                        <Button
                            icon={<Trash2Icon className="h-4 text-destructive" />}
                            onClick={() => {
                                setCurrentApiKey(info.row.original);
                                setShowDeleteDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        />
                    </>
                ),
                header: '',
                id: 'actions',
            }),
        ],
        [setCurrentApiKey, setShowDeleteDialog, setShowEditDialog]
    );

    const reactTable = useReactTable<ApiKey>({
        columns,
        data: apiKeys,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
            <Table>
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
                                        cell.id.endsWith('enabled') && 'ml-6 flex',
                                        cell.id.endsWith('name') && 'truncate xl:min-w-80'
                                    )}
                                    key={cell.id}
                                    onClick={(event) => {
                                        if (cell.id.endsWith('actions')) {
                                            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                            const target = event.target as any;

                                            if (target.getAttribute('data-action') === 'delete') {
                                                setCurrentApiKey(apiKeys[target.getAttribute('data-index')]);

                                                setShowDeleteDialog(true);
                                            } else if (target.getAttribute('data-action') === 'edit') {
                                                setCurrentApiKey(apiKeys[target.getAttribute('data-index')]);

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
        </div>
    );
};

export default ApiKeyTable;
