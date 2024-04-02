import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {ApiKeyModel} from '@/middleware/platform/user';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {twMerge} from 'tailwind-merge';

const columnHelper = createColumnHelper<ApiKeyModel>();

const columns = [
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
        cell: (info) => `${info.getValue()?.toLocaleDateString()} ${info.getValue()?.toLocaleTimeString()}`,
        header: 'Last Used Date',
    }),
    columnHelper.display({
        cell: (info) => (
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button size="icon" variant="ghost">
                        <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                    </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem data-action="edit" data-index={info.row.index.toString()}>
                        Edit
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
];

interface ApiKeyTableProps {
    apiKeys: ApiKeyModel[];
}

const ApiKeyTable = ({apiKeys}: ApiKeyTableProps) => {
    const reactTable = useReactTable<ApiKeyModel>({
        columns,
        data: apiKeys,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full space-y-8 px-4 2xl:w-4/5">
            <p className="pb-4">
                Your secret API keys are listed below. Please note that we do not display your secret API keys again
                after you generate them.
            </p>

            <p>
                Do not share your API key with others, or expose it in the browser or other client-side code. In order
                to protect the security of your account, ByteChef may also automatically disable any API key that we
                have found has leaked publicly.
            </p>

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
                                        'whitespace-nowrap p-3',
                                        cell.id.endsWith('actions') && 'flex justify-end',
                                        cell.id.endsWith('enabled') && 'flex ml-6'
                                    )}
                                    key={cell.id}
                                    onClick={(event) => {
                                        if (cell.id.endsWith('actions')) {
                                            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                            const target = event.target as any;

                                            if (target.getAttribute('data-action') === 'delete') {
                                                console.log(target.getAttribute('data-action'));
                                            } else if (target.getAttribute('data-action') === 'edit') {
                                                console.log(target.getAttribute('data-action'));
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
