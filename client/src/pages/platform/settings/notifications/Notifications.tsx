import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {useGetNotificationsQuery} from '@/ee/queries/notifications.queries';
import NotificationDialog from '@/pages/platform/settings/notifications/components/NotificationDialog';
import NotificationsList from '@/pages/platform/settings/notifications/components/NotificationsList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Link2Icon} from 'lucide-react';

const Notifications = () => {
    const {
        data: notifications,
        error: notificationsError,
        isLoading: notificationsIsLoading,
    } = useGetNotificationsQuery();

    return (
        <LayoutContainer
            header={
                notifications &&
                notifications.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<NotificationDialog triggerNode={<Button>New Notification</Button>} />}
                        title="Notifications"
                    />
                )
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[notificationsError]} loading={notificationsIsLoading}>
                {notifications && notifications.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
                        <NotificationsList notifications={notifications} />
                    </div>
                ) : (
                    <EmptyList
                        button={<NotificationDialog triggerNode={<Button>New Notification</Button>} />}
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Notification created yet."
                        title="No Notifications"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Notifications;
