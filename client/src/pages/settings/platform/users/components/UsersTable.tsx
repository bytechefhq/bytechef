import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import UsersTableSkeleton from '@/pages/settings/platform/users/components/UsersTableSkeleton';
import {EditIcon, Trash2Icon} from 'lucide-react';
import {forwardRef, useImperativeHandle} from 'react';

import useUsersTable from './hooks/useUsersTable';

export interface UsersTableRefI {
    isLoading: boolean;
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
}

interface UsersTableProps {
    onOpenDelete: (login: string | null) => void;
    onOpenEdit: (login: string) => void;
    pageNumber: number;
}

const UsersTable = forwardRef<UsersTableRefI, UsersTableProps>(function UsersTable(
    {onOpenDelete, onOpenEdit, pageNumber},
    ref
) {
    const {error, isLoading, pageSize, totalElements, totalPages, users} = useUsersTable({pageNumber});

    useImperativeHandle(
        ref,
        () => ({
            isLoading,
            pageNumber,
            pageSize,
            totalElements,
            totalPages,
        }),
        [isLoading, pageNumber, pageSize, totalElements, totalPages]
    );

    if (error) {
        return <div className="text-destructive">Error: {(error as Error).message}</div>;
    }

    return (
        <Table>
            <TableHeader>
                <TableRow className="border-b-border/50">
                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Email
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Name
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Role
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Status
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"></TableHead>
                </TableRow>
            </TableHeader>

            <TableBody>
                {isLoading ? (
                    <UsersTableSkeleton />
                ) : (
                    <>
                        {users.map((user) => (
                            <TableRow className="cursor-pointer border-b-border/50" key={user?.id ?? user?.login}>
                                <TableCell className="whitespace-nowrap">{user?.email}</TableCell>

                                <TableCell className="whitespace-nowrap">
                                    {[user?.firstName, user?.lastName].filter(Boolean).join(' ')}
                                </TableCell>

                                <TableCell className="whitespace-nowrap">{user?.authorities?.[0] ?? ''}</TableCell>

                                <TableCell className="whitespace-nowrap">
                                    {user?.activated ? 'Active' : 'Pending'}
                                </TableCell>

                                <TableCell className="flex justify-end whitespace-nowrap">
                                    <Button onClick={() => onOpenEdit(user?.login ?? '')} size="icon" variant="ghost">
                                        <EditIcon className="size-4" />
                                    </Button>

                                    <Button
                                        onClick={() => onOpenDelete(user?.login ?? null)}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <Trash2Icon className="size-4 text-destructive" />
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}

                        {users.length === 0 && (
                            <TableRow>
                                <TableCell className="px-4 py-6 text-center text-muted-foreground" colSpan={5}>
                                    No users found.
                                </TableCell>
                            </TableRow>
                        )}
                    </>
                )}
            </TableBody>
        </Table>
    );
});

export default UsersTable;
