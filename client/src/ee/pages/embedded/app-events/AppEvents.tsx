import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import AppEventDialog from '@/ee/pages/embedded/app-events/components/AppEventDialog';
import AppEventList from '@/ee/pages/embedded/app-events/components/AppEventList';
import AppEventsFilterTitle from '@/ee/pages/embedded/app-events/components/AppEventsFilterTitle';
import {useGetAppEventsQuery} from '@/ee/shared/queries/embedded/appEvents.queries';
import {UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ZapIcon} from 'lucide-react';

const AppEvents = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const canCreateAppEventScope = useHasEmbeddedScope('APP_EVENT_CREATE', UNION_ENVIRONMENT_ID);

    const {data: appEvents, error: appEventsError, isLoading: appEventsIsLoading} = useGetAppEventsQuery();

    const canCreateAppEvent = canCreateAppEventScope && !currentEnvironmentId;

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
                                canCreateAppEvent &&
                                appEvents &&
                                appEvents.length > 0 && (
                                    <AppEventDialog triggerNode={<Button label="New App Event" />} />
                                )
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
                        button={canCreateAppEvent && <AppEventDialog triggerNode={<Button label="New App Event" />} />}
                        icon={<ZapIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={
                            canCreateAppEvent
                                ? 'Get started by creating a new app event.'
                                : 'There are no app events yet.'
                        }
                        title="No App Events"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AppEvents;
