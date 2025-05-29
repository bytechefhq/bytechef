import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import NotificationDialog from '@/pages/platform/settings/notifications/components/NotificationDialog';
import NotificationsTable from '@/pages/platform/settings/notifications/components/NotificationsTable';
import Header from '@/shared/layout/Header';
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
        <>
            <Header
                centerTitle
                position="main"
                right={
                    <Button
                        className="bg-surface-brand-primary hover:bg-surface-brand-primary-hover"
                        onClick={() => openEditDialog()}
                    >
                        <PlusIcon /> New Notification
                    </Button>
                }
                title="Notifications"
            />

            <PageLoader errors={[notificationsError]} loading={isNotificationsLoading}>
                {!notificationsData?.length && (
                    <EmptyList
                        button={
                            <Button
                                className="bg-surface-brand-primary hover:bg-surface-brand-primary-hover"
                                onClick={() => openEditDialog()}
                            >
                                <PlusIcon /> New Notification
                            </Button>
                        }
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Notifications created yet."
                        title="No Notifications"
                    />
                )}

                <NotificationsTable columns={columns as []} notifications={notificationsData!} />
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
        </>
    );
};

export default Notifications;
