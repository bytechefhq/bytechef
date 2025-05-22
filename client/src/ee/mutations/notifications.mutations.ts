import {Notification, NotificationApi} from '@/shared/middleware/platform/configuration';
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
