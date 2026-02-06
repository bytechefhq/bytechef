import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ApiConnectorCreateMenu from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorCreateMenu';
import ApiConnectorEndpointDetailPanel from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEndpointDetailPanel';
import ApiConnectorList from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useApiConnectorsQuery} from '@/shared/middleware/graphql';
import {Link2Icon} from 'lucide-react';

const ApiConnectors = () => {
    const {
        data: apiConnectorsData,
        error: apiConnectorsError,
        isLoading: apiConnectorsLoading,
    } = useApiConnectorsQuery();

    const apiConnectors = apiConnectorsData?.apiConnectors;

    return (
        <LayoutContainer
            header={
                apiConnectors &&
                apiConnectors.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={apiConnectors && apiConnectors.length > 0 && <ApiConnectorCreateMenu />}
                        title="API Connectors"
                    />
                )
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[apiConnectorsError]} loading={apiConnectorsLoading}>
                {apiConnectors && apiConnectors?.length > 0 ? (
                    <ApiConnectorList apiConnectors={apiConnectors} />
                ) : (
                    <EmptyList
                        button={<ApiConnectorCreateMenu />}
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any API Connectors created yet."
                        title="No API Connectors"
                    />
                )}
            </PageLoader>

            <ApiConnectorEndpointDetailPanel />
        </LayoutContainer>
    );
};

export default ApiConnectors;
