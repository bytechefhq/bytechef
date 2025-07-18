import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import AppEventDialog from '@/ee/pages/embedded/app-events/components/AppEventDialog';
import AppEventList from '@/ee/pages/embedded/app-events/components/AppEventList';
import AppEventsFilterTitle from '@/ee/pages/embedded/app-events/components/AppEventsFilterTitle';
import {useGetAppEventsQuery} from '@/ee/shared/queries/embedded/appEvents.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ZapIcon} from 'lucide-react';

const AppEvents = () => {
    const {data: appEvents, error: appEventsError, isLoading: appEventsIsLoading} = useGetAppEventsQuery();

    return (
        <PageLoader errors={[appEventsError]} loading={appEventsIsLoading}>
            <LayoutContainer
                header={
                    appEvents &&
                    appEvents.length > 0 && (
                        <Header
                            centerTitle={true}
                            position="main"
                            right={
                                appEvents &&
                                appEvents.length > 0 && <AppEventDialog triggerNode={<Button>New App Event</Button>} />
                            }
                            title={<AppEventsFilterTitle filterData={{}} workflows={[]} />}
                        />
                    )
                }
                leftSidebarBody={
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: true,
                                        name: 'All Workflows',
                                    }}
                                />
                            </>
                        }
                        title="Workflows"
                    />
                }
                leftSidebarHeader={<Header title="App Events" />}
                leftSidebarWidth="64"
            >
                {appEvents && appEvents.length > 0 ? (
                    <AppEventList appEvents={appEvents} />
                ) : (
                    <EmptyList
                        button={<AppEventDialog triggerNode={<Button>New App Event</Button>} />}
                        icon={<ZapIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new app event."
                        title="No App Events"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AppEvents;
