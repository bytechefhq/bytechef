import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Notification} from '@/shared/middleware/platform/notification';
import {flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {twMerge} from 'tailwind-merge';

const NotificationsTable = ({columns, notifications}: {columns: []; notifications: Notification[]}) => {
    const table = useReactTable({
        columns,
        data: notifications,
        getCoreRowModel: getCoreRowModel(),
    });

    return (
        <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
            <Table>
                <TableHeader>
                    {table.getHeaderGroups().map((headerGroup) => (
                        <TableRow key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <TableHead className="w-1/4 min-w-[25%] whitespace-nowrap" key={header.id}>
                                    {!header.isPlaceholder &&
                                        flexRender(header.column.columnDef.header, header.getContext())}
                                </TableHead>
                            ))}
                        </TableRow>
                    ))}
                </TableHeader>

                <TableBody>
                    {table.getRowModel().rows.map((row, index) => (
                        <TableRow
                            className={twMerge(
                                'border-b border-stroke-neutral-secondary hover:bg-transparent',
                                index % 2 === 1 && 'bg-gray-50 hover:bg-gray-50'
                            )}
                            key={row.id}
                        >
                            {row.getVisibleCells().map((cell) => (
                                <TableCell key={cell.id}>
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

export default NotificationsTable;
