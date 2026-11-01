import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import AppEventDialog from '@/ee/pages/embedded/app-events/components/AppEventDialog';
import AppEventList from '@/ee/pages/embedded/app-events/components/AppEventList';
import AppEventsFilterTitle from '@/ee/pages/embedded/app-events/components/AppEventsFilterTitle';
import {EMBEDDED_APP_EVENTS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {useGetAppEventsQuery} from '@/ee/shared/queries/embedded/appEvents.queries';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ZapIcon} from 'lucide-react';

const AppEvents = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: appEvents, error: appEventsError, isLoading: appEventsIsLoading} = useGetAppEventsQuery();

    const initialLoadComplete = useInitialLoadComplete(appEventsIsLoading);

    const isFirstRun = appEvents?.length === 0;

    return (
        <PageLoader errors={[appEventsError]} loading={appEventsIsLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            appEvents &&
                            appEvents.length > 0 && (
                                <AppEventDialog
                                    triggerNode={!currentEnvironmentId ? <Button label="New App Event" /> : <></>}
                                />
                            )
                        }
                        title={
                            appEvents && appEvents.length > 0 ? (
                                <AppEventsFilterTitle filterData={{}} workflows={[]} />
                            ) : (
                                isFirstRun && 'App Events'
                            )
                        }
                    />
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
                leftSidebarHidden={!initialLoadComplete || isFirstRun}
                leftSidebarWidth="64"
            >
                {appEvents && appEvents.length > 0 ? (
                    <AppEventList appEvents={appEvents} />
                ) : (
                    <EmptyState
                        actions={
                            !currentEnvironmentId && (
                                <AppEventDialog triggerNode={<Button label="Create App Event" />} />
                            )
                        }
                        description="An app event is something that happens in your product, like a new sign-up or a placed order. Declare it here with the shape of its data, then any integration workflow can start when your product fires it."
                        docsLink={{href: EMBEDDED_APP_EVENTS_DOCUMENTATION_URL, label: 'How app events work'}}
                        icon={<ZapIcon />}
                        title="Create your first app event"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AppEvents;
