import {Notification, NotificationApi, NotificationSigningSecret} from '@/shared/middleware/platform/notification';
import {useMutation} from '@tanstack/react-query';

interface CreateNotificationMutationProps {
    onError?: (error: Error, variables: Notification) => void;
    onSuccess?: (result: Notification, variables: Notification) => void;
}

export const useCreateNotificationMutation = (mutationProps?: CreateNotificationMutationProps) =>
    useMutation<Notification, Error, Notification>({
        mutationFn: (notification: Notification) => {
            return new NotificationApi().createNotification({
                notification,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteNotificationMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteNotificationMutation = (mutationProps?: DeleteNotificationMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (notificationId: number) => {
            return new NotificationApi().deleteNotification({
                notificationId,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateNotificationMutationProps {
    onError?: (error: Error, variables: Notification) => void;
    onSuccess?: (result: Notification, variables: Notification) => void;
}

export const useUpdateNotificationMutation = (mutationProps?: UpdateNotificationMutationProps) =>
    useMutation<Notification, Error, Notification>({
        mutationFn: (notification: Notification) => {
            return new NotificationApi().updateNotification({
                notification,
                notificationId: notification.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface RegenerateNotificationSigningSecretMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: NotificationSigningSecret, variables: number) => void;
}

export const useRegenerateNotificationSigningSecretMutation = (
    mutationProps?: RegenerateNotificationSigningSecretMutationProps
) =>
    useMutation<NotificationSigningSecret, Error, number>({
        mutationFn: (notificationId: number) => {
            return new NotificationApi().regenerateNotificationSigningSecret({
                notificationId,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
