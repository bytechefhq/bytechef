import NotificationsListItem from '@/pages/platform/settings/notifications/components/NotificationsListItem';
import {Notification} from '@/shared/middleware/platform/configuration';

const NotificationsList = ({notifications}: {notifications: Notification[]}) => {
    return (
        <ul className="w-full divide-y divide-gray-100 px-2 2xl:mx-auto 2xl:w-4/5" role="list">
            {notifications.map((notification) => {
                return <NotificationsListItem key={notification.id} notification={notification} />;
            })}
        </ul>
    );
};

export default NotificationsList;
