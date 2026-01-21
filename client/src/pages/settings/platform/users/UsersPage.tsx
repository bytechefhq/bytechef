import PrimaryButton from '@/components/Button/Button';
import TablePagination from '@/components/TablePagination';
import DeleteUserAlertDialog from '@/pages/settings/platform/users/components/DeleteUserAlertDialog';
import EditUserDialog from '@/pages/settings/platform/users/components/EditUserDialog';
import InviteUserDialog from '@/pages/settings/platform/users/components/InviteUserDialog';
import UsersTable from '@/pages/settings/platform/users/components/UsersTable';
import useInviteUserDialog from '@/pages/settings/platform/users/components/hooks/useInviteUserDialog';
import useUsersTable from '@/pages/settings/platform/users/components/hooks/useUsersTable';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useState} from 'react';

export default function UsersPage() {
    const [pageNumber, setPageNumber] = useState(0);

    const {isLoading, pageSize, totalElements, totalPages} = useUsersTable({pageNumber});
    const {handleOpen: handleOpenInvite} = useInviteUserDialog();

    const handlePaginationClick = (newPageNumber: number) => {
        setPageNumber(newPageNumber);
    };

    return (
        <LayoutContainer
            footer={
                totalElements > 0 && (
                    <Footer position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={pageNumber}
                            pageSize={pageSize}
                            totalElements={totalElements}
                            totalPages={totalPages}
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
                        <PrimaryButton disabled={isLoading} onClick={handleOpenInvite}>
                            Invite User
                        </PrimaryButton>
                    }
                    title="Users"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
                <UsersTable pageNumber={pageNumber} />
            </div>

            <DeleteUserAlertDialog />

            <InviteUserDialog />

            <EditUserDialog />
        </LayoutContainer>
    );
}
