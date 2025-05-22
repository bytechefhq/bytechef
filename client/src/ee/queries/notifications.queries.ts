import {Notification, NotificationApi} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const NotificationKeys = ['notifications'];

export const useGetNotificationsQuery = () =>
    useQuery<Notification[], Error>({
        queryFn: () => new NotificationApi().getNotifications(),
        queryKey: NotificationKeys,
    });
