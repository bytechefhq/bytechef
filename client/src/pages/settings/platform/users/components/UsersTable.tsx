import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import UsersTableSkeleton from '@/pages/settings/platform/users/components/UsersTableSkeleton';
import useDeleteUserAlertDialog from '@/pages/settings/platform/users/components/hooks/useDeleteUserAlertDialog';
import useEditUserDialog from '@/pages/settings/platform/users/components/hooks/useEditUserDialog';
import {EditIcon, Trash2Icon} from 'lucide-react';

import useUsersTable from './hooks/useUsersTable';

interface UsersTableProps {
    pageNumber: number;
}

const UsersTable = ({pageNumber}: UsersTableProps) => {
    const {error, isLoading, users} = useUsersTable({pageNumber});
    const {handleOpen: handleOpenDelete} = useDeleteUserAlertDialog();
    const {handleOpen: handleOpenEdit} = useEditUserDialog();

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
                                    <Button
                                        icon={<EditIcon className="size-4" />}
                                        onClick={() => handleOpenEdit(user?.login ?? '')}
                                        size="icon"
                                        variant="ghost"
                                    />

                                    <Button
                                        icon={<Trash2Icon className="size-4 text-destructive" />}
                                        onClick={() => handleOpenDelete(user?.login ?? null)}
                                        size="icon"
                                        variant="ghost"
                                    />
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
};

export default UsersTable;
