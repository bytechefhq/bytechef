import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import NotificationDialog from '@/pages/settings/platform/notifications/components/NotificationDialog';
import NotificationsTable from '@/pages/settings/platform/notifications/components/NotificationsTable';
import SigningSecretDialog from '@/pages/settings/platform/notifications/components/SigningSecretDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Link2Icon, PlusIcon} from 'lucide-react';
import {UseFormReturn} from 'react-hook-form';

import useNotifications, {NotificationFormValuesType} from './hooks/useNotifications';

const Notifications = () => {
    const isEnterpriseEdition = useApplicationInfoStore((state) => state.application?.edition === EditionType.EE);

    const {
        closeDeleteDialog,
        closeEditDialog,
        closeSigningSecretDialog,
        columns,
        form,
        handleDeleteNotification,
        isDeleteDialogOpen,
        isEditDialogOpen,
        isNotificationsLoading,
        notificationsData,
        notificationsError,
        openEditDialog,
        regenerateSigningSecret,
        saveNotification,
        selectedNotification,
        signingSecret,
    } = useNotifications();

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description={
                        isEnterpriseEdition
                            ? 'Send an email, Slack message, or webhook when a workflow run reaches a given status.'
                            : 'Send an email or Slack message when a workflow run reaches a given status.'
                    }
                    position="main"
                    right={<Button icon={<PlusIcon />} label="New Notification" onClick={() => openEditDialog()} />}
                    title="Notifications"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[notificationsError]} loading={isNotificationsLoading}>
                {notificationsData && notificationsData?.length > 0 ? (
                    <NotificationsTable columns={columns} notifications={notificationsData!} />
                ) : (
                    <EmptyList
                        button={
                            <Button icon={<PlusIcon />} label="New Notification" onClick={() => openEditDialog()} />
                        }
                        icon={<Link2Icon className="size-12 text-content-neutral-tertiary" />}
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
                    onRegenerateSigningSecret={regenerateSigningSecret}
                    saveNotification={saveNotification}
                    selectedNotification={selectedNotification}
                />
            )}

            {signingSecret && <SigningSecretDialog onClose={closeSigningSecretDialog} signingSecret={signingSecret} />}

            {isDeleteDialogOpen && !!selectedNotification && (
                <AlertDialog
                    description="This action cannot be undone."
                    onCancel={closeDeleteDialog}
                    onConfirm={() => handleDeleteNotification(selectedNotification.id!)}
                    open={isDeleteDialogOpen}
                    title={`Delete ${selectedNotification.name} notification?`}
                />
            )}
        </LayoutContainer>
    );
};

export default Notifications;
