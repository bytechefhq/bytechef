import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import ApiConnectorEndpointDetailPanel from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEndpointDetailPanel';
import ApiConnectorList from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorList';
import ComponentPoliciesTab from '@/ee/pages/settings/platform/component-policies/ComponentPoliciesTab';
import ComponentVisibilityTab from '@/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab';
import NewComponentMenu from '@/ee/pages/settings/platform/components/components/NewComponentMenu';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useApiConnectorsQuery, useCustomComponentsQuery} from '@/shared/middleware/graphql';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Link2Icon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

export type ComponentsTabType = 'api-connectors' | 'component-visibility' | 'custom' | 'policies';

interface ComponentsProps {
    tab: ComponentsTabType;
}

/**
 * Merged home for custom components and API connectors (an API connector is a kind of custom component). Each tab
 * keeps its existing list, row menus, and detail flows untouched; only the page shell, the tab bar, and the shared
 * New Component menu are new. The tab is a subroute (components/custom, components/api-connectors) so deep links and
 * the connector-wizard return path keep working.
 */
const Components = ({tab}: ComponentsProps) => {
    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const navigate = useNavigate();

    const {
        data: customComponentsData,
        error: customComponentsError,
        isLoading: customComponentsLoading,
    } = useCustomComponentsQuery(undefined, {enabled: tab === 'custom'});

    const {
        data: apiConnectorsData,
        error: apiConnectorsError,
        isLoading: apiConnectorsLoading,
    } = useApiConnectorsQuery(undefined, {enabled: tab === 'api-connectors'});

    const apiConnectors = apiConnectorsData?.apiConnectors;
    const customComponents = customComponentsData?.customComponents;

    const showTabs = isFeatureFlagEnabled('ff-207') && isFeatureFlagEnabled('ff-1024');

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" right={<NewComponentMenu />} title="Components" />}
            leftSidebarOpen={false}
        >
            <Tabs className="size-full" onValueChange={(value) => navigate(`../${value}`)} value={tab}>
                {showTabs && (
                    <TabsList className="mx-6 mt-2 mb-2">
                        <TabsTrigger value="component-visibility">Component Visibility</TabsTrigger>

                        <TabsTrigger value="custom">Custom Components</TabsTrigger>

                        <TabsTrigger value="api-connectors">API Connectors</TabsTrigger>
                    </TabsList>
                )}

                <TabsContent value="policies">
                    <ComponentPoliciesTab />
                </TabsContent>

                <TabsContent value="component-visibility">
                    <div className="w-full px-6 3xl:mx-auto 3xl:w-4/5">
                        <ComponentVisibilityTab />
                    </div>
                </TabsContent>

                <TabsContent className="flex flex-col" value="custom">
                    <PageLoader errors={[customComponentsError]} loading={customComponentsLoading}>
                        {customComponents && customComponents.length > 0 ? (
                            <CustomComponentList customComponents={customComponents} />
                        ) : (
                            <div className="flex flex-1 items-center justify-center">
                                <EmptyList
                                    button={<NewComponentMenu />}
                                    icon={<Link2Icon className="size-12 text-content-neutral-tertiary" />}
                                    message="You do not have any Custom Components created yet."
                                    title="No Custom Components"
                                />
                            </div>
                        )}
                    </PageLoader>
                </TabsContent>

                <TabsContent className="flex flex-col" value="api-connectors">
                    <PageLoader errors={[apiConnectorsError]} loading={apiConnectorsLoading}>
                        {apiConnectors && apiConnectors.length > 0 ? (
                            <ApiConnectorList apiConnectors={apiConnectors} />
                        ) : (
                            <div className="flex flex-1 items-center justify-center">
                                <EmptyList
                                    button={<NewComponentMenu />}
                                    icon={<Link2Icon className="size-12 text-content-neutral-tertiary" />}
                                    message="You do not have any API Connectors created yet."
                                    title="No API Connectors"
                                />
                            </div>
                        )}
                    </PageLoader>

                    <ApiConnectorEndpointDetailPanel />
                </TabsContent>
            </Tabs>
        </LayoutContainer>
    );
};

export default Components;
