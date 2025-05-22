import {NotificationEvent, NotificationEventApi} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const NotificationEventKeys = ['notificationEvents'];

export const useGetNotificationEventsQuery = () =>
    useQuery<NotificationEvent[], Error>({
        queryFn: () => new NotificationEventApi().getNotificationEvents(),
        queryKey: NotificationEventKeys,
    });
