import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import AppEventDialog from '@/pages/embedded/app-events/components/AppEventDialog';
import AppEventList from '@/pages/embedded/app-events/components/AppEventList';
import {useGetAppEventsQuery} from '@/queries/embedded/appEvents.queries';
import {ZapIcon} from 'lucide-react';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';

const AppEvents = () => {
    const {data: appEvents, error: appEventsError, isLoading: appEventsIsLoading} = useGetAppEventsQuery();

    return (
        <PageLoader errors={[appEventsError]} loading={appEventsIsLoading}>
            <LayoutContainer
                header={
                    <PageHeader
                        centerTitle={true}
                        position="main"
                        right={
                            appEvents &&
                            appEvents.length > 0 && <AppEventDialog triggerNode={<Button>New App Event</Button>} />
                        }
                        title="Workflows: All"
                    />
                }
                leftSidebarBody={
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        filterData: true,
                                        name: 'All Workflows',
                                    }}
                                />
                            </>
                        }
                        title="Workflows"
                    />
                }
                leftSidebarHeader={<PageHeader title="App Events" />}
            >
                {appEvents && appEvents.length > 0 ? (
                    <AppEventList appEvents={appEvents} />
                ) : (
                    <EmptyList
                        button={<AppEventDialog triggerNode={<Button>New App Event</Button>} />}
                        icon={<ZapIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new app event."
                        title="No App Events"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AppEvents;
