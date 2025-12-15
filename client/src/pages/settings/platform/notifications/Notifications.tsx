import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import NotificationDialog from '@/pages/settings/platform/notifications/components/NotificationDialog';
import NotificationsTable from '@/pages/settings/platform/notifications/components/NotificationsTable';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Link2Icon, PlusIcon} from 'lucide-react';
import {UseFormReturn} from 'react-hook-form';

import NotificationDeleteDialog from './components/NotificationDeleteDialog';
import useNotifications, {NotificationFormValuesType} from './hooks/useNotifications';

const Notifications = () => {
    const {
        closeDeleteDialog,
        closeEditDialog,
        columns,
        form,
        handleDeleteNotification,
        isDeleteDialogOpen,
        isEditDialogOpen,
        isNotificationsLoading,
        notificationsData,
        notificationsError,
        openEditDialog,
        saveNotification,
        selectedNotification,
    } = useNotifications();

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    position="main"
                    right={<Button icon={<PlusIcon />} label="New Notification" onClick={() => openEditDialog()} />}
                    title="Notifications"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[notificationsError]} loading={isNotificationsLoading}>
                {notificationsData && notificationsData?.length > 0 ? (
                    <NotificationsTable columns={columns as []} notifications={notificationsData!} />
                ) : (
                    <EmptyList
                        button={
                            <Button icon={<PlusIcon />} label="New Notification" onClick={() => openEditDialog()} />
                        }
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Notifications created yet."
                        title="No Notifications"
                    />
                )}
            </PageLoader>

            {isEditDialogOpen && (
                <NotificationDialog
                    closeEditDialog={closeEditDialog}
                    form={
                        form as unknown as UseFormReturn<
                            NotificationFormValuesType,
                            unknown,
                            NotificationFormValuesType
                        >
                    }
                    isEditDialogOpen={isEditDialogOpen}
                    saveNotification={saveNotification}
                    selectedNotification={selectedNotification}
                />
            )}

            {isDeleteDialogOpen && !!selectedNotification && (
                <NotificationDeleteDialog
                    closeDeleteDialog={closeDeleteDialog}
                    handleDeleteNotification={handleDeleteNotification}
                    isDeleteDialogOpen={isDeleteDialogOpen}
                    selectedNotification={selectedNotification}
                />
            )}
        </LayoutContainer>
    );
};

export default Notifications;
