import PrimaryButton from '@/components/Button/Button';
import TablePagination from '@/components/TablePagination';
import DeleteUserAlertDialog, {
    DeleteUserAlertDialogRefI,
} from '@/pages/settings/platform/users/components/DeleteUserAlertDialog';
import EditUserDialog, {EditUserDialogRefI} from '@/pages/settings/platform/users/components/EditUserDialog';
import InviteUserDialog, {InviteUserDialogRefI} from '@/pages/settings/platform/users/components/InviteUserDialog';
import UsersTable, {UsersTableRefI} from '@/pages/settings/platform/users/components/UsersTable';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useRef, useState} from 'react';

export default function UsersPage() {
    const [pageNumber, setPageNumber] = useState(0);

    const deleteUserAlertDialogRef = useRef<DeleteUserAlertDialogRefI>(null);
    const editUserDialogRef = useRef<EditUserDialogRefI>(null);
    const inviteUserDialogRef = useRef<InviteUserDialogRefI>(null);
    const usersTableRef = useRef<UsersTableRefI>(null);

    const handleOpenDelete = (login: string | null) => {
        deleteUserAlertDialogRef.current?.open(login);
    };

    const handleOpenEdit = (login: string) => {
        editUserDialogRef.current?.open(login);
    };

    const handleOpenInvite = () => {
        inviteUserDialogRef.current?.open();
    };

    const handlePaginationClick = (newPageNumber: number) => {
        setPageNumber(newPageNumber);
    };

    return (
        <LayoutContainer
            footer={
                usersTableRef.current &&
                usersTableRef.current.totalElements > 0 && (
                    <Footer position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={pageNumber}
                            pageSize={usersTableRef.current.pageSize}
                            totalElements={usersTableRef.current.totalElements}
                            totalPages={usersTableRef.current.totalPages}
                        />
                    </Footer>
                )
            }
            header={
                <Header
                    centerTitle
                    description="Manage organization users: invite or delete users."
                    position="main"
                    right={
                        <PrimaryButton disabled={usersTableRef.current?.isLoading ?? false} onClick={handleOpenInvite}>
                            Invite User
                        </PrimaryButton>
                    }
                    title="Users"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
                <UsersTable
                    onOpenDelete={handleOpenDelete}
                    onOpenEdit={handleOpenEdit}
                    pageNumber={pageNumber}
                    ref={usersTableRef}
                />
            </div>

            <DeleteUserAlertDialog ref={deleteUserAlertDialogRef} />

            <InviteUserDialog ref={inviteUserDialogRef} />

            <EditUserDialog ref={editUserDialogRef} />
        </LayoutContainer>
    );
}
